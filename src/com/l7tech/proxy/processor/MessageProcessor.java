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
import java.security.cert.X509Certificate;

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

    public MessageProcessor(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

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
     * @param req   the PendingRequest to decorate
     * @throws PolicyAssertionException     if the policy evaluation could not be completed
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
     * @param ssg   the Ssg to which we are sending our application
     * @throws GeneralSecurityException   if there was a problem making the CSR
     * @throws GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws IOException                if there was a network problem
     * @throws IllegalArgumentException   if no credentials are configured for this Ssg
     */
    private void obtainClientCertificate(Ssg ssg) throws GeneralSecurityException, IOException {
        if (!ssg.isCredentialsConfigured())
            throw new IllegalArgumentException("need credentials to apply for a certificate");
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
    }

    /**
     * Get the appropriate Ssg URL for forwarding the given request.
     * @param req
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
     * @param req
     * @return
     * @throws ConfigurationException
     */
    private SsgResponse obtainResponse(PendingRequest req)
            throws ConfigurationException, IOException,
                   PolicyRetryableException, ServerCertificateUntrustedException, OperationCanceledException
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
                throw new PolicyRetryableException();
            }

            Header contentType = postMethod.getResponseHeader("Content-Type");
            log.info("Response Content-Type: " + contentType);
            if (contentType == null || contentType.getValue() == null || contentType.getValue().indexOf("text/xml") < 0)
                return new SsgResponse(CannedSoapFaults.RESPONSE_NOT_XML);

            SsgResponse response = new SsgResponse(postMethod.getResponseBodyAsString());
            log.info("Got response from SSG: " + response);
            if (status == 401 || status == 500) {
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
     * next attempt to connect to the SSG via SSL should succeed.
     *
     * @throws java.security.KeyStoreException if the SSG key could not be stored in our trustStore
     */
    private void installSsgServerCertificate(Ssg ssg)
            throws IOException, GeneralSecurityException, OperationCanceledException
    {
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             ssg.getUsername(),
                                                             ssg.password());
        if (!ssg.isCredentialsConfigured())
            if (!Managers.getCredentialManager().getCredentials(ssg))
                throw new OperationCanceledException("Client Proxy user declined to provide credentials.");

        cd.setUsername(ssg.getUsername());
        cd.setPassword(ssg.password());

        if (cd.downloadCertificate()) {
            SsgKeyStoreManager.saveSsgCertificate(ssg, (X509Certificate) cd.getCertificate());
            return; // Success.
        }

        Managers.getCredentialManager().notifyInvalidCredentials(ssg);
        if (!Managers.getCredentialManager().getCredentials(ssg))
            throw new OperationCanceledException("User declined to provide credentials");
    }
}
