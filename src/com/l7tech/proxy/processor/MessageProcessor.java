/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.processor;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.SsgSessionManager;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.PolicyRetryableException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.util.CannedSoapFaults;
import com.l7tech.util.XmlUtil;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Category;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * The core of the Client Proxy.
 *
 * User: mike
 * Date: Aug 13, 2003
 * Time: 9:51:36 AM
 */
public class MessageProcessor {
    private static final Category log = Category.getInstance(MessageProcessor.class);
    private static final int MAX_TRIES = 8;
    private PolicyManager policyManager;

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
     * @throws PolicyAssertionException     if the policy evaluation of request or response could not be completed due
     *                                        to a serious error
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException   if the user declined to provide a username and password
     * @throws ConfigurationException       if a response could not be obtained from the SSG due to a problem with
     *                                      the client or server configuration, and retrying the operation
     *                                      is unlikely to succeed until the configuration is changed.
     * @throws ConfigurationException       if we were unable to conform to the policy, and did not get any useful
     *                                      SOAP fault from the SSG.
     * @throws GeneralSecurityException     if the SSG SSL certificate could not be obtained or installed
     * @throws IOException                  if information couldn't be obtained from the SSG due to network trouble
     * @throws IOException                  if a certificate could not be saved to disk
     */
    public SsgResponse processMessage(PendingRequest req)
            throws ClientCertificateException, PolicyAssertionException, OperationCanceledException,
                   ConfigurationException, GeneralSecurityException, IOException
    {
        Ssg ssg = req.getSsg();

        for (int attempts = 0; attempts < MAX_TRIES; ++attempts) {
            try {
                try {
                    Policy appliedPolicy = enforcePolicy(req);
                    SsgResponse res = obtainResponse(req, appliedPolicy);
                    undecorateResponse(req, res, appliedPolicy);
                    return res;
                } catch (SSLException e) {
                    if (ExceptionUtils.causedBy(e, BadCredentialsException.class) ||
                        ExceptionUtils.causedBy(e, UnrecoverableKeyException.class))
                        throw new BadCredentialsException(e);

                    if (ExceptionUtils.causedBy(e, ServerCertificateUntrustedException.class))
                        installSsgServerCertificate(ssg); // might throw BadCredentialsException
                        // allow policy to reset and retry
                    else
                        // not sure what happened; throw it up and abort the request
                        throw e;
                } catch (ServerCertificateUntrustedException e) {
                    installSsgServerCertificate(ssg); // might throw BadCredentialsException
                    // allow policy to reset and retry
                }
            } catch (PolicyRetryableException e) {
                // allow policy to reset and retry
            } catch (BadCredentialsException e) {
                // If we have a client cert, and the current password worked to decrypt it's private key, but something
                // has rejected the password anyway, we need to reestablish the validity of this account with the SSG.
                if (SsgKeyStoreManager.isClientCertAvailabile(ssg) && SsgKeyStoreManager.isPasswordWorkedForPrivateKey(ssg)) {
                    if (securePasswordPing(ssg)) {
                        // password works with our keystore, and with the SSG, so why did it fail just now?
                        String message = "Recieved password failure, but it worked with our keystore and the SSG liked it when we double-checked it.  " +
                                  "Could be an internal error, or an attack, or the SSG admin toggling your account on and off.";
                        log.error(message);
                        throw new ConfigurationException(message, e);
                    } else {
                        log.error("The SSG password that was used to obtain this client cert is no longer valid -- deleting the client cert");
                        SsgKeyStoreManager.deleteClientCert(ssg);
                        req.getClientProxy().initializeSsl(); // reset global SSL state
                    }
                }

                Managers.getCredentialManager().notifyInvalidCredentials(ssg);
                Managers.getCredentialManager().getCredentials(ssg);
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
     * Contact the SSG and determine whether this SSG password is still valid.
     *
     * @param ssg  the SSG to contact.  must be configured with the credentials you wish to validate
     * @return  true if these credentials appear to be valid on this SSG; false otherwise
     * @throws IOException if we were unable to validate this password either way
     */
    private boolean securePasswordPing(Ssg ssg) throws IOException {
        // We'll just use the CertificateDownloader for this.
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             ssg.getUsername(),
                                                             ssg.password());
        try {
            boolean worked = cd.downloadCertificate();
            ssg.passwordWorkedWithSsg(worked);
            return worked;
        } catch (CertificateException e) {
            log.error("SSG sent us an invalid certificate during secure password ping", e);
            throw new IOException("SSG sent us an invalid certificate during secure password ping");
        }
    }

    /**
     * Massage the provided PendingRequest so it conforms to the policy for this operation for the
     * associated Ssg.
     *
     * @param req   the PendingRequest to decorate
     * @throws PolicyAssertionException     if the policy evaluation could not be completed due to a serious error
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException   if the user declined to provide a username and password
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException  if the Ssg certificate needs to be (re)imported.
     */
    private Policy enforcePolicy(PendingRequest req)
            throws PolicyAssertionException, OperationCanceledException,
            ServerCertificateUntrustedException, BadCredentialsException
    {
        Policy policy = policyManager.getPolicy(req);
        if (policy == null)
            return null;

        AssertionStatus result;
        try {
            result = policy.getClientAssertion().decorateRequest(req);
        } catch (PolicyAssertionException e) {
            if (e.getCause() instanceof BadCredentialsException || e.getCause() instanceof UnrecoverableKeyException)
                throw new BadCredentialsException(e);
            throw e;
        }
        if (result != AssertionStatus.NONE)
            log.warn("Policy evaluated with an error: " + result + "; will attempt to continue anyway.");
        return policy;
    }

    /**
     * Process the response from the SSG, stripping any stuff the end user client doesn't care about or shouldn't
     * see, according to the dictates of the policy for this request.
     *
     * @param req
     * @param res
     * @param appliedPolicy                 the policy that was applied to the original request.
     * @throws PolicyAssertionException     if the policy evaluation could not be completed due to a serious error
     */
    private void undecorateResponse(PendingRequest req, SsgResponse res, Policy appliedPolicy)
            throws PolicyAssertionException, OperationCanceledException,
                   ServerCertificateUntrustedException, BadCredentialsException
    {
        log.info(appliedPolicy == null ? "skipping undecorate step" : "undecorating response");
        if (appliedPolicy == null)
            return;
        AssertionStatus result = appliedPolicy.getClientAssertion().unDecorateReply(req, res);
        if (result != AssertionStatus.NONE)
            log.warn("Response policy processing failed with status " + result + "; continuing anyway");
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
                if (req.isSslForbidden())
                    throw new ConfigurationException("SSL is both required and forbidden by policy");
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
     * @throws IOException            if there was a network problem getting the message response from the SSG
     * @throws IOException            if there was a network problem downloading a policy from the SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.PolicyRetryableException if a new policy was downloaded
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException if a policy couldn't be downloaded because the SSG SSL certificate
     *                                             was not recognized and needs to be (re)imported
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if credentials were needed to continue processing, but the user canceled
     *                                    the logon dialog (or we are running headless).
     * @throws com.l7tech.proxy.datamodel.exceptions.ClientCertificateException if our client cert is no longer valid, but we couldn't delete it from
     *                                    the keystore.
     * @throws com.l7tech.proxy.datamodel.exceptions.BadCredentialsException if the SSG rejected our SSG username and/or password.
     */
    private SsgResponse obtainResponse(PendingRequest req, Policy policy)
            throws ConfigurationException, IOException, PolicyRetryableException, ServerCertificateUntrustedException,
            OperationCanceledException, ClientCertificateException, BadCredentialsException
    {
        URL url = getUrl(req);
        Ssg ssg = req.getSsg();

        HttpClient client = new HttpClient();
        HttpState state = client.getState();
        PostMethod postMethod = null;
        try {
            postMethod = new PostMethod(url.toString());
            setAuthenticationState(req, state, postMethod);
            postMethod.addRequestHeader("SOAPAction", req.getSoapAction());
            postMethod.addRequestHeader(SecureSpanConstants.HttpHeaders.ORIGINAL_URL, req.getOriginalUrl().toString());
            if (req.isNonceRequired())
                postMethod.addRequestHeader(SecureSpanConstants.HttpHeaders.XML_NONCE_HEADER_NAME,
                                            Long.toString(req.getNonce()));
            if (req.getSession() != null)
                postMethod.addRequestHeader(SecureSpanConstants.HttpHeaders.XML_SESSID_HEADER_NAME,
                                            Long.toString(req.getSession().getId()));
            if (policy != null)
                postMethod.addRequestHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION, policy.getVersion());

            String postBody = XmlUtil.documentToString(req.getSoapEnvelopeDirectly());
            //log.info("Posting to SSG: " + postBody);
            postMethod.setRequestBody(postBody);

            log.info("Posting request to SSG " + ssg + ", url " + url);
            int status = client.executeMethod(postMethod);
            log.info("POST to SSG completed with HTTP status code " + status);

            Header sessionStatus = postMethod.getResponseHeader(SecureSpanConstants.HttpHeaders.SESSION_STATUS_HTTP_HEADER);
            if (sessionStatus != null) {
                log.info("SSG response contained a session status header: " + sessionStatus.getName() + ": " + sessionStatus.getValue());
                if (sessionStatus.getValue().equalsIgnoreCase("invalid")) {
                    log.info("sessionstatus:invalid header; will invalidate session and try again");
                    SsgSessionManager.invalidateSession(ssg);
                    throw new PolicyRetryableException();
                }
            } else
                log.info("SSG response contained no session status header");

            Header policyUrlHeader = postMethod.getResponseHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER);
            if (policyUrlHeader != null) {
                log.info("SSG response contained a PolicyUrl header: " + policyUrlHeader.getValue());
                // Have we already updated a policy while processing this request?
                if (req.isPolicyUpdated())
                    throw new ConfigurationException("Policy was updated, but SSG says it's still out-of-date");
                URL policyUrl;
                try {
                    policyUrl = new URL(policyUrlHeader.getValue());
                    // force the policy URL to point at the SSG hostname the user typed
                    policyUrl = new URL(policyUrl.getProtocol(), ssg.getSsgAddress(), policyUrl.getPort(), policyUrl.getFile());
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
                req.setLastErrorResponse(response);
                Header authHeader = postMethod.getResponseHeader("WWW-Authenticate");
                log.info("Got auth header: " + authHeader.getValue());
                if (authHeader == null && SsgKeyStoreManager.isClientCertAvailabile(ssg) && "https".equals(url.getProtocol())) {
                    // 401 without an auth challenge, if the connection was made successfully over SSL,
                    // means we should delete our client certificate and retry.
                    log.info("SSG indicates that our client certificate is no longer valid; deleting it");
                    try {
                        SsgKeyStoreManager.deleteClientCert(ssg);
                        req.getClientProxy().initializeSsl(); // flush all global SSL state
                    } catch (Exception e) {
                        throw new ClientCertificateException(e);
                    }
                }

                throw new BadCredentialsException();
            }

            return response;

        } finally {
            if (postMethod != null)
                postMethod.releaseConnection();
        }
    }

    /**
     * Configure HTTP Basic or Digest auth on the specified HttpState and PostMethod, if called for by
     * the specified PendingRequest.
     *
     * @param req  the PendingRequest that might require HTTP level authentication
     * @param state  the HttpState to adjust
     * @param postMethod  the PostMethod to adjust
     */
    private void setAuthenticationState(PendingRequest req, HttpState state, PostMethod postMethod)
            throws OperationCanceledException
    {
        state.setAuthenticationPreemptive(false);

        if (!req.isBasicAuthRequired() && !req.isDigestAuthRequired()) {
            log.info("No HTTP Basic or Digest authentication required by current policy");
            return;
        }

        Ssg ssg = req.getSsg();
        String username;
        String password;
        synchronized (ssg) {
            if (!ssg.isCredentialsConfigured())
                Managers.getCredentialManager().getCredentials(ssg);
            username = ssg.getUsername();
            password = new String(ssg.password());
        }

        if (req.isBasicAuthRequired() || req.isDigestAuthRequired()) {
            log.info("Enabling HTTP Basic or Digest auth with username=" + username);
            postMethod.setDoAuthentication(true);
            state.setAuthenticationPreemptive(true);
            state.setCredentials(null, null,
                                 new UsernamePasswordCredentials(username, password));
        }
    }

    /**
     * Get credentials, and download and install the SSG certificate.  If this completes successfully, the
     * next attempt to connect to the SSG via SSL should at least get past the SSL handshake.
     *
     * @throws IOException if there was a network problem downloading the server cert
     * @throws IOException if there was a problem reading or writing the keystore for this SSG
     * @throws BadCredentialsException if the downloaded cert could not be verified with the SSG username and password
     * @throws OperationCanceledException if credentials were needed but the user declined to enter them
     * @throws GeneralSecurityException for miscellaneous and mostly unlikely certificate or key store problems
     */
    private void installSsgServerCertificate(Ssg ssg)
            throws IOException, BadCredentialsException, OperationCanceledException, GeneralSecurityException
    {
        if (!ssg.isCredentialsConfigured())
            Managers.getCredentialManager().getCredentials(ssg);

        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             ssg.getUsername(),
                                                             ssg.password());

        if (cd.downloadCertificate()) {
            SsgKeyStoreManager.saveSsgCertificate(ssg, (X509Certificate) cd.getCertificate());
            return; // Success.
        }

        throw new BadCredentialsException("Unable to verify server certificate with the current username and password for SSG " + ssg);
    }
}
