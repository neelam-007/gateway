/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.processor;

import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.SslUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.util.CannedSoapFaults;
import com.l7tech.proxy.util.ThreadLocalHttpClient;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Category;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

/**
 * The core of the Client Proxy.
 * User: mike
 * Date: Aug 13, 2003
 * Time: 9:51:36 AM
 */
public class MessageProcessor {
    private static final Category log = Category.getInstance(MessageProcessor.class);
    private PolicyManager policyManager;
    private static final int MAX_TRIES = 6;

    /**
     * Construct a Client Proxy MessageProcessor.
     *
     * @param policyManager The PolicyManager to be used to determine what policy to apply to a given request
     */
    public MessageProcessor(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    /**
     * Process the given client request and return the corresponding response.
     *
     * @param req  the PendingRequest to process
     * @return     the SsgResponse containing the response from the Ssg, if processing was successful, or if
     *             a SOAP fault is being returned to the client from either the CP or the SSG.
     * @throws ClientCertificateException   if a client certificate was required but could not be obtained
     * @throws PolicyAssertionException     if the policy evaluation could not be completed due to a serious error
     * @throws OperationCanceledException   if the user declined to provide a username and password
     * @throws ConfigurationException       if a response could not be obtained from the SSG due to a problem with
     *                                      the client or server configuration, and retrying the operation
     *                                      is unlikely to succeed until the configuration is changed.
     * @throws ConfigurationException       if we were unable to conform to the policy, and did not get any useful
     *                                      SOAP fault from the SSG.
     * @throws GeneralSecurityException     if the SSG SSL certificate could not be obtained or installed
     * @throws IOException                  if a information couldn't be obtained from the SSG due to network trouble
     * @throws IOException                  if a certificate could not be saved to disk
     */
    public SsgResponse processMessage(PendingRequest req)
            throws ClientCertificateException, PolicyAssertionException, OperationCanceledException,
                   ConfigurationException, GeneralSecurityException, IOException
    {
        for (int attempts = 0; attempts < MAX_TRIES; ++attempts) {
            try {
                enforcePolicy(req);
                return obtainResponse(req);
            } catch (ServerCertificateUntrustedException e) {
                installSsgServerCertificate(req.getSsg());
                // allow policy to reset and retry
            } catch (PolicyRetryableException e) {
                // allow policy to reset and retry
            }
            req.reset();
        }

        log.warn("Too many attempts to conform to policy; giving up");
        if (req.getLastErrorResponse() != null)
            return req.getLastErrorResponse();
        throw new ConfigurationException("Unable to conform to policy, and no useful fault from Ssg.");
    }

    /**
     * Massage the provided PendingRequest so it conforms to the policy for this operation for the
     * associated Ssg.
     *
     * @param req   the PendingRequest to decorate
     * @throws PolicyAssertionException     if the policy evaluation could not be completed due to a serious error
     * @throws PolicyRetryableException     if the policy evaluation should be started over
     * @throws OperationCanceledException   if the user declined to provide a username and password
     * @throws ClientCertificateException   if a client certificate was required but could not be obtained
     * @throws ServerCertificateUntrustedException  if the Ssg certificate needs to be (re)imported.
     */
    private void enforcePolicy(PendingRequest req)
            throws PolicyAssertionException, PolicyRetryableException, OperationCanceledException,
                   ClientCertificateException, ServerCertificateUntrustedException
    {
        Ssg ssg = req.getSsg();
        ClientAssertion policy = policyManager.getClientPolicy(req);
        AssertionStatus result = policy.decorateRequest(req);
        if (result != AssertionStatus.NONE) {
            if (req.isCredentialsWouldHaveHelped() || req.isClientCertWouldHaveHelped()) {
                boolean gotCreds = false;
                if (!ssg.isCredentialsConfigured()) {
                    if (!Managers.getCredentialManager().getCredentials(ssg))
                        throw new OperationCanceledException("User of Client Proxy declined to provide credentials.");
                    gotCreds = true;
                }
                if (req.isClientCertWouldHaveHelped()) {
                    if (gotCreds) {
                        log.info("Policy failed, possibly due to lack of client certificate, possibly because we just couldn't decrypt it yet.  Will try again now that we know the password.");
                        throw new PolicyRetryableException();
                    }

                    log.info("Policy failed, possibly due to lack of a client certificate.  Will request one, then try again.");
                    try {
                        obtainClientCertificate(ssg);
                    } catch (ServerCertificateUntrustedException e) {
                        throw e;
                    } catch (SSLHandshakeException e) {
                        if (e.getCause() instanceof ServerCertificateUntrustedException)
                            throw (ServerCertificateUntrustedException) e.getCause();
                        throw new ClientCertificateException("Couldn't obtain a client certificate", e);
                    } catch (GeneralSecurityException e) {
                        throw new ClientCertificateException("Couldn't obtain a client certificate", e);
                    } catch (IOException e) {
                        throw new ClientCertificateException("Couldn't obtain a client certificate", e);
                    }
                }
                throw new PolicyRetryableException();
            }
            log.warn("Policy evaluated with an error: " + result + "; will attempt to continue anyway.");
        }
    }

    /**
     * Generate a Certificate Signing Request, and apply to the Ssg for a certificate for the
     * current user.  If this method returns, the certificate will have been downloaded and saved
     * locally.
     *
     * @param ssg   the Ssg to which we are sending our application
     * @throws GeneralSecurityException   if there was a problem making the CSR
     * @throws GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws IOException                if there was a network problem
     * @throws IllegalArgumentException   if no credentials are configured for this Ssg
     */
    private void obtainClientCertificate(Ssg ssg) throws GeneralSecurityException, IOException {
        if (!ssg.isCredentialsConfigured())
            throw new IllegalArgumentException("need credentials to apply for a certificate");
        log.info("Generating new RSA key pair (could take several seconds)...");
        Managers.getCredentialManager().notifyLengthyOperationStarting(ssg, "Generating new client certificate...");
        JDKKeyPairGenerator.RSA kpg = new JDKKeyPairGenerator.RSA();
        KeyPair keyPair = kpg.generateKeyPair();
        PKCS10CertificationRequest csr = SslUtils.makeCsr(ssg.getUsername(),
                                                          keyPair.getPublic(),
                                                          keyPair.getPrivate());
        X509Certificate cert = SslUtils.obtainClientCertificate(ssg.getServerCertRequestUrl(),
                                                                ssg.getUsername(),
                                                                ssg.password(),
                                                                csr);
        SsgKeyStoreManager.saveClientCertificate(ssg, keyPair.getPrivate(), cert);
        Managers.getCredentialManager().notifyLengthyOperationFinished(ssg);
    }

    /**
     * Get the appropriate Ssg URL for forwarding the given request.
     *
     * @param req  the PendingRequest to process
     * @return a URL that might have had it's protocol and port modified if SSL is indicated.
     * @throws ConfigurationException if we couldn't find a valid URL in this Ssg configuration.
     */
    private URL getUrl(PendingRequest req) throws ConfigurationException {
        Ssg ssg = req.getSsg();
        URL url = null;
        try {
            url = ssg.getServerUrl();
            if (req.isSslRequired()) {
                if ("http".equalsIgnoreCase(url.getProtocol())) {
                    log.info("Changing http to https per policy for this request (using SSL port " +
                             ssg.getSslPort() + ")");
                    url = new URL("https", url.getHost(), ssg.getSslPort(), url.getFile());
                } else
                    throw new ConfigurationException("Couldn't find an SSL-enabled version of protocol " +
                                                     url.getProtocol());
            }
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Client Proxy: SSG \"" + ssg + "\" has an invalid server url: " +
                                             ssg.getServerUrl());
        }

        return url;
    }

    /**
     * Call the Ssg and obtain its response to the current message.
     *
     * @param req the PendingRequest to process
     * @return the SsgResponse from the SSG
     * @throws ConfigurationException if the SSG url is invalid
     * @throws ConfigurationException if the we downloaded a new policy for this request, but we are still being told
     *                                the policy is out-of-date
     * @throws ConfigurationException if the SSG sends us an invalid Policy URL
     * @throws ConfigurationException if the PendingRequest did not contain enough information to construct a
     *                                valid PolicyAttachmentKey
     * @throws IOException            if there was a network problem communicating with the SSG
     * @throws IOException            if there was a network problem downloading a policy from the SSG
     * @throws PolicyRetryableException if a new policy was downloaded
     * @throws PolicyRetryableException if new credentials were obtained from the user
     * @throws ServerCertificateUntrustedException if a policy couldn't be downloaded because the SSG SSL certificate
     *                                             was not recognized and needs to be (re)imported
     * @throws OperationCanceledException if credentials were needed to continue processing, but the user canceled
     *                                    the logon dialog (or we are running headless).
     * @throws ClientCertificateException if our client cert is no longer valid, but we couldn't delete it from
     *                                    the keystore.
     */
    private SsgResponse obtainResponse(PendingRequest req)
            throws ConfigurationException, IOException, PolicyRetryableException, ServerCertificateUntrustedException,
                   OperationCanceledException, ClientCertificateException
    {
        URL url = getUrl(req);
        Ssg ssg = req.getSsg();

        HttpClient client = ThreadLocalHttpClient.getHttpClient();
        HttpState state = client.getState();
        PostMethod postMethod = null;
        try {
            postMethod = new PostMethod(url.toString());
            setAuthenticationState(req, state, postMethod);

            postMethod.addRequestHeader("SOAPAction", req.getSoapAction());
            postMethod.setRequestBody(req.getSoapEnvelope().toString());

            log.info("Posting request to SSG " + ssg + ", url " + url);
            int status = client.executeMethod(postMethod);
            log.info("POST to SSG completed with HTTP status code " + status);

            Header policyUrlHeader = postMethod.getResponseHeader("PolicyUrl");
            if (policyUrlHeader != null) {
                log.info("SSG response contained a PolicyUrl header: " + policyUrlHeader.getValue());
                // Have we already updated a policy while processing this request?
                if (req.isPolicyUpdated())
                    throw new ConfigurationException("Policy was updated, but SSG says it's still out-of-date");
                URL policyUrl;
                try {
                    policyUrl = new URL(policyUrlHeader.getValue());
                } catch (MalformedURLException e) {
                    throw new ConfigurationException("The Ssg sent us an invalid Policy URL.");
                }
                postMethod.releaseConnection(); // free up our thread's HTTP client
                postMethod = null;
                policyManager.updatePolicy(req, policyUrl);
                req.setPolicyUpdated(true);
                if (status != 200) {
                    log.info("Retrying request with the new policy");
                    throw new PolicyRetryableException();
                }
                log.info("Will use new policy for future requests.");
            }

            Header contentType = postMethod.getResponseHeader("Content-Type");
            log.info("Response Content-Type: " + contentType);
            if (contentType == null || contentType.getValue() == null || contentType.getValue().indexOf("text/xml") < 0)
                return new SsgResponse(CannedSoapFaults.RESPONSE_NOT_XML);

            SsgResponse response = new SsgResponse(postMethod.getResponseBodyAsString());
            log.info("Got response from SSG: " + response);
            if (status == 401) {
                Header authHeader = postMethod.getResponseHeader("WWW-Authenticate");
                if (authHeader == null && SsgKeyStoreManager.isClientCertAvailabile(ssg) && "https".equals(url.getProtocol())) {
                    // 401 without an auth challenge, if the connection was made successfully over SSL,
                    // means we should delete our client certificate and retry.
                    log.info("SSG indicates that our client certificate is no longer valid; deleting it");
                    try {
                        SsgKeyStoreManager.deleteClientCert(ssg);
                        throw new PolicyRetryableException();
                    } catch (NoSuchAlgorithmException e) {
                        // can't happen
                        throw new ClientCertificateException(e);
                    } catch (KeyStoreException e) {
                        throw new ClientCertificateException(e);
                    } catch (CertificateException e) {
                        throw new ClientCertificateException(e);
                    }
                }

                req.setLastErrorResponse(response);
                Managers.getCredentialManager().notifyInvalidCredentials(ssg);
                if (!Managers.getCredentialManager().getCredentials(req.getSsg()))
                    throw new OperationCanceledException("User declined to provide credentials");
                throw new PolicyRetryableException();
            }

            return response;

        } finally {
            if (postMethod != null)
                postMethod.releaseConnection();
        }
    }

    /**
     * Configure HTTP Basic or Digest auth on the specific HttpState and PostMethod, if called for by
     * the specified PendingRequest.
     *
     * @param req  the PendingRequest that might require HTTP level authentication
     * @param state  the HttpState to adjust
     * @param postMethod  the PendingRequest to adjust
     */
    private void setAuthenticationState(PendingRequest req, HttpState state, PostMethod postMethod) {
        state.setAuthenticationPreemptive(false);
        if (req.isBasicAuthRequired()) {
            log.info("Enabling HTTP Basic auth with username=" + req.getHttpBasicUsername());
            postMethod.setDoAuthentication(true);
            state.setAuthenticationPreemptive(true);
            state.setCredentials(null, null,
                                 new UsernamePasswordCredentials(req.getHttpBasicUsername(),
                                                                 new String(req.getHttpBasicPassword())));
        } else if (req.isDigestAuthRequired()) {
            log.info("Enabling HTTP Digest auth with username=" + req.getHttpDigestUsername());
            postMethod.setDoAuthentication(true);
            state.setCredentials(null, null,
                                 new UsernamePasswordCredentials(req.getHttpDigestUsername(),
                                                                 new String(req.getHttpDigestPassword())));
        } else
            log.info("No HTTP Basic or Digest authentication required by current policy");
    }


    /**
     * Get credentials, and download and install the SSG certificate.  If this completes successfully, the
     * next attempt to connect to the SSG via SSL should at least get past the SSL handshake.
     *
     * @throws java.security.KeyStoreException if the SSG key could not be stored in our trustStore
     */
    private void installSsgServerCertificate(Ssg ssg)
            throws IOException, GeneralSecurityException, OperationCanceledException
    {
        if (!ssg.isCredentialsConfigured())
            if (!Managers.getCredentialManager().getCredentials(ssg))
                throw new OperationCanceledException("Client Proxy user declined to provide credentials.");

        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             ssg.getUsername(),
                                                             ssg.password());

        if (cd.downloadCertificate()) {
            SsgKeyStoreManager.saveSsgCertificate(ssg, (X509Certificate) cd.getCertificate());
            return; // Success.
        }

        Managers.getCredentialManager().notifyInvalidCredentials(ssg);
        if (!Managers.getCredentialManager().getCredentials(ssg))
            throw new OperationCanceledException("User declined to provide credentials");
    }
}
