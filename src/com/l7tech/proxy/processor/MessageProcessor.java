/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.processor;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.SsgSessionManager;
import com.l7tech.proxy.datamodel.HttpHeaders;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.ClientCertificateException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.PolicyRetryableException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.ssl.HostnameMismatchException;
import com.l7tech.proxy.util.CannedSoapFaults;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Category;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

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
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException   if the user declined to provide a username and password
     * @throws ConfigurationException       if a response could not be obtained from the SSG due to a problem with
     *                                      the client or server configuration, and retrying the operation
     *                                      is unlikely to succeed until the configuration is changed.
     * @throws ConfigurationException       if we were unable to conform to the policy, and did not get any useful
     *                                      SOAP fault from the SSG.
     * @throws GeneralSecurityException     if the SSG SSL certificate could not be obtained or installed
     * @throws IOException                  if information couldn't be obtained from the SSG due to network trouble
     * @throws IOException                  if a certificate could not be saved to disk
     * @throws SAXException                 if the client request needed to be parsed and wasn't well-formed XML
     * @throws ResponseValidationException  if the response was signed, but the signature did not validate
     */
    public SsgResponse processMessage(PendingRequest req)
            throws ClientCertificateException, OperationCanceledException,
                   ConfigurationException, GeneralSecurityException, IOException, SAXException,
                   ResponseValidationException
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
                    // An SSL handshake with the SSG has failed.
                    // See if we can identify the cause and correct it.

                    // Do we not have the right password to access our keystore?
                    if (ExceptionUtils.causedBy(e, BadCredentialsException.class) ||
                        ExceptionUtils.causedBy(e, UnrecoverableKeyException.class))
                        throw new BadCredentialsException(e);

                    // Was this server cert untrusted?
                    if (!ExceptionUtils.causedBy(e, ServerCertificateUntrustedException.class)) {
                        // No, that wasn't the problem.  Was it a cert hostname mismatch?
                        HostnameMismatchException hme = (HostnameMismatchException)
                                        ExceptionUtils.getCauseIfCausedBy(e, HostnameMismatchException.class);
                        if (hme != null) {
                            // Notify user of the hostname mismatch and then abort this request
                            String wanted = hme.getWhatWasWanted();
                            String got = hme.getWhatWeGotInstead();
                            Managers.getCredentialManager().notifySsgHostnameMismatch(ssg,
                                                                                      wanted,
                                                                                      got);
                            throw e;
                        }

                        // not sure what happened; throw it up and abort the request
                        throw e;
                    }

                    // We don't trust the server cert.  Perform certificate discovery and try again
                    ClientProxy.installSsgServerCertificate(ssg); // might throw BadCredentialsException
                    // FALLTHROUGH -- retry with new server certificate
                } catch (ServerCertificateUntrustedException e) {
                    ClientProxy.installSsgServerCertificate(ssg); // might throw BadCredentialsException
                    // FALLTHROUGH allow policy to reset and retry
                }
            } catch (PolicyRetryableException e) {
                // FALLTHROUGH allow policy to reset and retry
            } catch (BadCredentialsException e) {
                // If we have a client cert, and the current password worked to decrypt it's private key, but something
                // has rejected the password anyway, we need to reestablish the validity of this account with the SSG.
                if (SsgKeyStoreManager.isClientCertAvailabile(ssg) && SsgKeyStoreManager.isPasswordWorkedForPrivateKey(ssg)) {
                    if (securePasswordPing(req)) {
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

                req.getNewCredentials();
                // FALLTHROUGH allow policy to reset and retry
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
     * @param req request we are processing.  must be configured with the credentials you wish to validate
     * @return  true if these credentials appear to be valid on this SSG; false otherwise
     * @throws IOException if we were unable to validate this password either way
     * @throws OperationCanceledException if a logon dialog appeared and the user canceled it;
     *                                    this shouldn't happen unless the user clears the credentials
     */
    private boolean securePasswordPing(PendingRequest req)
            throws IOException, OperationCanceledException
    {
        Ssg ssg = req.getSsg();
        // We'll just use the CertificateDownloader for this.
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             req.getUsername(),
                                                             req.getPassword());
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
     * @throws OperationCanceledException   if the user declined to provide a username and password
     * @throws ServerCertificateUntrustedException  if the Ssg certificate needs to be (re)imported.
     */
    private Policy enforcePolicy(PendingRequest req)
            throws OperationCanceledException, GeneralSecurityException, BadCredentialsException, IOException, SAXException, ClientCertificateException
    {
        Policy policy = policyManager.getPolicy(req);
        if (policy == null)
            return null;

        AssertionStatus result;
        result = policy.getClientAssertion().decorateRequest(req);
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
     */
    private void undecorateResponse(PendingRequest req, SsgResponse res, Policy appliedPolicy)
            throws OperationCanceledException,
                   GeneralSecurityException, BadCredentialsException, IOException,
                   ResponseValidationException, SAXException
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
                    log.error("Error: SSL is both forbidden and required by policy -- leaving SSL enabled");
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

            Header certStatusHeader = postMethod.getResponseHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS);
            if (certStatusHeader != null && "invalid".equalsIgnoreCase(certStatusHeader.getValue())) {
                log.info("SSG response contained a certficate status:invalid header.  Will get new client cert.");
                // Try to get a new client cert; if this succeeds, it'll replace the old one
                try {
                    req.getClientProxy().obtainClientCertificate(req);
                    throw new PolicyRetryableException(); // try again with the new cert
                } catch (GeneralSecurityException e) {
                    throw new ClientCertificateException("Unable to obtain new client certificate", e);
                } catch (IOException e) {
                    throw new ClientCertificateException("Unable to obtain new client certificate", e);
                }
            }

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

                policyManager.updatePolicy(req, policyUrl);
                req.setPolicyUpdated(true);
                if (status != 200) {
                    log.info("Retrying request with the new policy");
                    throw new PolicyRetryableException();
                }
                log.info("Will use new policy for future requests.");
            }

            HttpHeaders headers = new HttpHeaders(postMethod.getResponseHeaders());

            Header contentType = postMethod.getResponseHeader("Content-Type");
            log.info("Response Content-Type: " + contentType);
            if (contentType == null || contentType.getValue() == null || contentType.getValue().indexOf("text/xml") < 0)
                return new SsgResponse(CannedSoapFaults.RESPONSE_NOT_XML, null);

            SsgResponse response = new SsgResponse(postMethod.getResponseBodyAsString(), headers);
            log.info("Got response from SSG: " + response);
            if (status == 401) {
                req.setLastErrorResponse(response);
                Header authHeader = postMethod.getResponseHeader("WWW-Authenticate");
                log.info("Got auth header: " + (authHeader == null ? "<null>" : authHeader.getValue()));
                if (authHeader == null && "https".equals(url.getProtocol()) && SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                    log.info("Got 401 response from SSG over https; possible that client cert is no good");
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

        String username = req.getUsername();
        char[] password = req.getPassword();
        if (req.isBasicAuthRequired() || req.isDigestAuthRequired()) {
            log.info("Enabling HTTP Basic or Digest auth with username=" + username);
            postMethod.setDoAuthentication(true);
            state.setAuthenticationPreemptive(req.isBasicAuthRequired());
            state.setCredentials(null, null,
                                 new UsernamePasswordCredentials(username, new String(password)));
        }
    }
}
