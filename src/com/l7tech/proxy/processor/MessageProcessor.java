/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.processor;

import com.l7tech.common.message.AbstractHttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.DecoratorException;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.HttpHeadersKnob;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.proxy.ssl.ClientProxySecureProtocolSocketFactory;
import com.l7tech.proxy.ssl.HostnameMismatchException;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.net.ssl.SSLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The core of the Client Proxy.
 * <p/>
 * User: mike
 * Date: Aug 13, 2003
 * Time: 9:51:36 AM
 */
public class MessageProcessor {
    private static final Logger log = Logger.getLogger(MessageProcessor.class.getName());

    public static final String PROPERTY_LOGPOSTS = "com.l7tech.proxy.processor.logPosts";
    public static final String PROPERTY_LOGRESPONSE = "com.l7tech.proxy.processor.logResponses";
    public static final String PROPERTY_LOGATTACHMENTS = "com.l7tech.proxy.processor.logAttachments";
    public static final String PROPERTY_REFORMATLOGGEDXML = "com.l7tech.proxy.processor.reformatLoggedXml";
    public static final String PROPERTY_TIMESTAMP_EXPIRY = "com.l7tech.proxy.processor.timestampExpiryMillis";
    public static final String PROPERTY_LOGPOLICIES    = "com.l7tech.proxy.datamodel.logPolicies";
    private static final Policy SSL_POLICY = new Policy(new SslAssertion(), null);
    private static Pattern findServiceid = Pattern.compile("^.*\\&?serviceoid=(.+?)(\\&.*|)", Pattern.DOTALL);

    private static final int MAX_TRIES = 8;
    private WssProcessor wssProcessor = new WssProcessorImpl();
    private WssDecorator wssDecorator = new WssDecoratorImpl();
    public static final String ENCODING = "UTF-8";

    static {
        // Configure SSL for outgoing connections
        System.setProperty("httpclient.useragent", SecureSpanConstants.USER_AGENT);
        Protocol https = new Protocol("https", ClientProxySecureProtocolSocketFactory.getInstance(), 443);
        Protocol.registerProtocol("https", https);
    }

    /**
     * Construct a Client Proxy MessageProcessor.
     */
    public MessageProcessor() {
    }

    /**
     * Process the given client request and return the corresponding response.
     *
     * @param context the PendingRequest to process
     * @throws ClientCertificateException     if a client certificate was required but could not be obtained
     * @throws OperationCanceledException     if the user declined to provide a username and password
     * @throws ConfigurationException         if a response could not be obtained from the SSG due to a problem with
     *                                        the client or server configuration, and retrying the operation
     *                                        is unlikely to succeed until the configuration is changed.
     * @throws ConfigurationException         if we were unable to conform to the policy, and did not get any useful
     *                                        SOAP fault from the SSG.
     * @throws GeneralSecurityException       if the SSG SSL certificate could not be obtained or installed
     * @throws IOException                    if information couldn't be obtained from the SSG due to network trouble
     * @throws IOException                    if a certificate could not be saved to disk
     * @throws SAXException                   if the client request needed to be parsed and wasn't well-formed XML
     * @throws ResponseValidationException    if the response was signed, but the signature did not validate
     * @throws HttpChallengeRequiredException if an HTTP 401 should be sent back to the client
     * @throws InvalidDocumentFormatException if the request or the response from the SSG had a problem with its
     *                                        format that was too serious to ignore
     * @throws com.l7tech.common.security.xml.processor.ProcessorException
     *                                        if there was a problem processing the wsse:Security header in the
     *                                        response from the SSG
     */
    public void processMessage(final PolicyApplicationContext context)
      throws ClientCertificateException, OperationCanceledException,
      ConfigurationException, GeneralSecurityException, IOException, SAXException,
      ResponseValidationException, HttpChallengeRequiredException, PolicyAssertionException,
      InvalidDocumentFormatException, ProcessorException, BadSecurityContextException
    {
        boolean succeeded = false;
        try {
            Ssg ssg = context.getSsg();

            for (int attempts = 0; attempts < MAX_TRIES; ++attempts) {
                CurrentRequest.setPeerSsg(null); // force all SSL connections to set peer SSG first
                try {
                    try {
                        try {
                            enforcePolicy(context);
                            obtainResponse(context);
                            undecorateResponse(context);
                            succeeded = true;
                            return;
                        } catch (SSLException e) {
                            if (context.getSsg().getTrustedGateway() != null) {
                                Ssg peerSsg = CurrentRequest.getPeerSsg();
                                if (peerSsg == context.getSsg())
                                    handleSslException(context.getSsg(), null, e);
                                else if (peerSsg == context.getSsg().getTrustedGateway())
                                    handleSslException(peerSsg, context.getFederatedCredentials(), e);
                                else
                                    throw new ConfigurationException("SSL handshake failed, but peer Gateway was neither the Trusted nor Federated Gateway.");
                            } else
                                handleSslException(context.getSsg(), context.getCredentialsForTrustedSsg(), e);
                            // FALLTHROUGH -- retry with new server certificate
                        } catch (ServerCertificateUntrustedException e) {
                            if (context.getSsg().getTrustedGateway() != null) {
                                Ssg peerSsg = CurrentRequest.getPeerSsg();
                                if (peerSsg == context.getSsg())
                                    SsgKeyStoreManager.installSsgServerCertificate(ssg, null);
                                else if (peerSsg == context.getSsg().getTrustedGateway())
                                    SsgKeyStoreManager.installSsgServerCertificate(peerSsg, context.getFederatedCredentials());
                                else
                                    throw new ConfigurationException("SSL handshake failed, but peer Gateway was neither the Trusted nor Federated Gateway.");
                            } else
                                SsgKeyStoreManager.installSsgServerCertificate(ssg, context.getCredentialsForTrustedSsg()); // might throw BadCredentialsException
                            // FALLTHROUGH allow policy to reset and retry
                        }
                    } catch (PolicyRetryableException e) {
                        // FALLTHROUGH allow policy to reset and retry
                    } catch (BadCredentialsException e) {
                        handleBadCredentialsException(ssg, context, e);
                        // FALLTHROUGH allow policy to reset and retry
                    }
                } catch (KeyStoreCorruptException e) {
                    Ssg problemSsg = ssg.getTrustedGateway();
                    if (problemSsg == null) problemSsg = ssg;
                    Managers.getCredentialManager().notifyKeyStoreCorrupt(problemSsg);
                    SsgKeyStoreManager.deleteStores(problemSsg);
                    ssg.resetSslContext();
                    // FALLTHROUGH -- retry, creating new keystore
                } catch (DecoratorException e) {
                    throw new ConfigurationException(e);
                }
                context.reset();
            }

            log.warning("Too many attempts to conform to policy; giving up");
            if (context.getResponse().isXml())
                return;
            throw new ConfigurationException("Unable to conform to policy, and no useful fault from Gateway.");
        } finally {
            if (!succeeded) {
                if (context.getActivePolicy() != null) {
                    log.log(Level.FINE, "Request failed to get a response from the SSG -- marking cached policy as invalid");
                    context.getActivePolicy().setValid(false);
                }
            }
        }
    }

    private void handleBadCredentialsException(Ssg ssg, PolicyApplicationContext context, BadCredentialsException e)
      throws KeyStoreCorruptException, IOException, OperationCanceledException,
            KeyStoreException, HttpChallengeRequiredException {
        if (ssg.isChainCredentialsFromClient())
            throw new HttpChallengeRequiredException(e);

        if (ssg.getTrustedGateway() != null)
            throw new OperationCanceledException("Client identity rejected by federated Gateway " + ssg.getSsgAddress());

        // If we have a client cert, and the current password worked to decrypt it's private key, but something
        // has rejected the password anyway, we need to reestablish the validity of this account with the SSG.
        if (SsgKeyStoreManager.isClientCertAvailabile(ssg) && SsgKeyStoreManager.isPasswordWorkedForPrivateKey(ssg)) {
            if (securePasswordPing(context)) {
                // password works with our keystore, and with the SSG, so why did it fail just now?
                String message = "Recieved password failure, but it worked with our keystore and the Gateway liked it when we double-checked it.  " +
                  "Most likely that your account exists but is not permitted to access this service.";
                log.severe(message);
                context.getNewCredentials();
                return;
            }
            log.severe("The Gateway password that was used to obtain this client cert is no longer valid -- deleting the client cert");
            try {
                SsgKeyStoreManager.deleteClientCert(ssg);
            } catch (KeyStoreCorruptException e1) {
                Ssg problemSsg = ssg.getTrustedGateway();
                if (problemSsg == null) problemSsg = ssg;
                Managers.getCredentialManager().notifyKeyStoreCorrupt(problemSsg);
                SsgKeyStoreManager.deleteStores(problemSsg);
                ssg.resetSslContext();
            }
            ssg.resetSslContext();
            // FALLTHROUGH -- retry, creating new keystore
        }

        context.getNewCredentials();
    }

    /**
     * Attempt to fix whatever caused the specified SSLException, if it was something simple like the server
     * cert being unknown.
     *
     * @param ssg
     * @param credentials
     * @param e
     * @throws BadCredentialsException
     * @throws IOException
     * @throws OperationCanceledException
     * @throws GeneralSecurityException
     * @throws KeyStoreCorruptException
     */
    public static void handleSslException(Ssg ssg, PasswordAuthentication credentials, SSLException e)
      throws BadCredentialsException, IOException, OperationCanceledException, GeneralSecurityException,
      KeyStoreCorruptException {
        // Do we not have the right password to access our keystore?
        if (ExceptionUtils.causedBy(e, BadCredentialsException.class) ||
          ExceptionUtils.causedBy(e, UnrecoverableKeyException.class)) {
            log.log(Level.INFO, "SSL handshake exception was apparently due to a password which doesn't match the keystore password", e);
            throw new BadCredentialsException(e);
        }

        // Was our keystore corrupt?
        Throwable ksce = ExceptionUtils.getCauseIfCausedBy(e, KeyStoreCorruptException.class);
        if (ksce != null)
            throw (KeyStoreCorruptException)ksce;

        // Was this server cert untrusted?
        Throwable scuet = ExceptionUtils.getCauseIfCausedBy(e, ServerCertificateUntrustedException.class);
        ServerCertificateUntrustedException scue = (ServerCertificateUntrustedException)scuet;
        if (scue == null) {
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
        SsgKeyStoreManager.installSsgServerCertificate(ssg, credentials); // might throw BadCredentialsException
    }

    /**
     * Contact the SSG and determine whether this SSG password is still valid.
     *
     * @param context request we are processing.  must be configured with the credentials you wish to validate
     * @return true if these credentials appear to be valid on this SSG; false otherwise
     * @throws IOException                if we were unable to validate this password either way
     * @throws OperationCanceledException if a logon dialog appeared and the user canceled it;
     *                                    this shouldn't happen unless the user clears the credentials
     */
    private boolean securePasswordPing(PolicyApplicationContext context)
      throws IOException, OperationCanceledException {
        Ssg ssg = context.getSsg();
        if (ssg.getTrustedGateway() != null)
            throw new OperationCanceledException("Unable to perform password ping with Federated SSG"); // can't happen

        // We'll just use the CertificateDownloader for this.
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
          context.getUsername(),
          context.getPassword());
        try {
            // TODO: remove this stupid hack.  it doesn't help LDAP users anyway
            boolean worked = cd.downloadCertificate();
            ssg.passwordWorkedWithSsg(worked);
            return worked;
        } catch (CertificateException e) {
            log.log(Level.SEVERE, "Gateway sent us an invalid certificate during secure password ping", e);
            throw new IOException("Gateway sent us an invalid certificate during secure password ping");
        }
    }

    /**
     * Massage the provided PendingRequest so it conforms to the policy for this operation for the
     * associated Ssg.  On return, the PendingRequest's activePolicy will be set to the policy
     * we applied.
     *
     * @param context the PendingRequest to decorate
     * @throws OperationCanceledException if the user declined to provide a username and password
     * @throws ServerCertificateUntrustedException
     *                                    if the Ssg certificate needs to be (re)imported.
     */
    private void enforcePolicy(PolicyApplicationContext context)
      throws OperationCanceledException, GeneralSecurityException, BadCredentialsException,
      IOException, SAXException, ClientCertificateException, KeyStoreCorruptException,
      HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException,
      InvalidDocumentFormatException, DecoratorException, ConfigurationException {
        Policy policy = lookupPolicy(context);
        if (policy == null || !policy.isValid()) {
            if (policy != null)
                log.warning("Ignoring this policy -- it has previously failed to complete successfully.");
            if (context.getSsg().isUseSslByDefault()) {
                // Use a default policy requiring SSL.
                policy = SSL_POLICY;
            } else {
                // No policy found for this request.
                context.setActivePolicy(null);
                return;
            }
        }

        context.setActivePolicy(policy);
        AssertionStatus result;
        ClientAssertion rootAssertion = policy.getClientAssertion();
        if (rootAssertion != null) {
            try {
                result = rootAssertion.decorateRequest(context);

                // Do any deferred decorations that weren't rolled back
                if (result == AssertionStatus.NONE) {
                    Map deferredDecorations = context.getPendingDecorations();
                    for (Iterator i = deferredDecorations.values().iterator(); i.hasNext();) {
                        ClientDecorator decorator = (ClientDecorator)i.next();
                        result = decorator.decorateRequest(context);
                        if (result != AssertionStatus.NONE)
                            break;
                    }
                }

                if (result == AssertionStatus.NONE) {
                    // Ensure L7a:MessageID exists if we are supposed to have one
                    final Message request = context.getRequest();
                    final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
                    if (context.getL7aMessageId() != null)
                        if (SoapUtil.getL7aMessageId(requestDoc) == null)
                            SoapUtil.setL7aMessageId(requestDoc, context.getL7aMessageId());

                    // Do all WSS processing all at once
                    if (request.isSoap()) {
                        log.info("Running pending request through WS-Security decorator");
                        Date ts = context.getSsg().dateTranslatorToSsg().translate(new Date());
                        Integer expiryMillis = Integer.getInteger(PROPERTY_TIMESTAMP_EXPIRY);
                        DecorationRequirements[] wssrequirements = context.getAllDecorationRequirements();
                        for (int i = 0; i < wssrequirements.length; i++) {
                            DecorationRequirements wssrequirement = wssrequirements[i];
                            wssrequirement.setTimestampCreatedDate(ts);
                            if (expiryMillis != null) {
                                wssrequirement.setTimestampTimeoutMillis(expiryMillis.intValue());
                            }
                            wssDecorator.decorateMessage(request.getXmlKnob().getDocumentWritable(), // upgrade to writable document
                                                         wssrequirement);
                        }
                    } else {
                        log.info("Request isn't SOAP; skipping WS-Security decoration");
                    }
                }

            } catch (PolicyAssertionException e) {
                // Before rethrowing, make sure we deactivate this cached policy.
                policy.setValid(false);
                throw e;
            }
            if (result != AssertionStatus.NONE) {
                log.warning("Policy evaluated with an error: " + result + "; aborting");
                throw new ConfigurationException("Unable to decorate request; policy evaluated with error: " + result);
            }
        }
    }

    private Policy lookupPolicy(PolicyApplicationContext context) {
        Policy policy = context.getSsg().rootPolicyManager().getPolicy(context.getPolicyAttachmentKey());
        if (policy != null) {
            if (LogFlags.logPolicies)
                log.info("Found a policy for this request: " + policy.getAssertion());
            else
                log.info("Found a policy for this request");
        } else
            log.info("No policy found for this request");
        return policy;
    }

    /**
     * Process the response from the SSG, stripping any stuff the end user client doesn't care about or shouldn't
     * see, according to the dictates of the policy for this request.
     *
     * @param context the request we are processing
     */
    private void undecorateResponse(PolicyApplicationContext context)
      throws OperationCanceledException,
      GeneralSecurityException, BadCredentialsException, IOException,
      ResponseValidationException, SAXException, KeyStoreCorruptException, PolicyAssertionException,
      ConfigurationException, InvalidDocumentFormatException, PolicyRetryableException
    {
        Policy appliedPolicy = context.getActivePolicy();
        log.info(appliedPolicy == null
                    ? "No policy applied to the request.  Leaving response undecorated"
                    : "undecorating response");
        if (appliedPolicy == null)
            return;

        Message response = context.getResponse();
        SoapFaultDetail responseFaultDetail = null;
        if (response.isSoap() && response.getSoapKnob().isFault())
            responseFaultDetail = response.getSoapKnob().getFaultDetail();

        // TODO refactor this fault handling into a routine of its own.  Should also deal with fault signing then.
        // Bug #1026 - If request used WS-SecureConversation, check if response is a BadContextToken fault.
        if (context.getSecureConversationId() != null) {
            if (responseFaultDetail != null && responseFaultDetail.getFaultCode() != null)
            {
                final String faultCode = responseFaultDetail.getFaultCode().trim();
                if (faultCode.equals(SecureSpanConstants.FAULTCODE_BADCONTEXTTOKEN)) {
                    // TODO we should only trust this fault if it is signed
                    log.info("Gateway reports " + SecureSpanConstants.FAULTCODE_BADCONTEXTTOKEN +
                             ".  Will throw away the current WS-SecureConversationSession and try again.");
                    context.closeSecureConversationSession();
                    throw new PolicyRetryableException("Flushed bad secure conversation session.");
                }
            }
            // TODO should we reject the response if isn't SOAP, but the request used WS-SC
            // FALLTHROUGH: not handled by agent -- fall through and send it back to the client
        }

        if (responseFaultDetail != null &&
                SecureSpanConstants.FAULTCODE_INVALIDSECURITYTOKEN.equals(responseFaultDetail.getFaultCode()))
        {
            // Invalid security token.  See if we need to drop a cached SAML assertion.
            // We'll assume that, if we sent a SAML token with the request, it is that token being
            // complained about.
            // TODO check faultDetail for SamlFaultInfo then only drop our token if the AssertionID matches
            if (context.getRequest().getXmlKnob().getOrMakeDecorationRequirements().getSenderSamlToken() != null) {
                // TODO we should only trust this fault if it is signed
                log.warning("Gateway reports " + responseFaultDetail.getFaultCode() +
                            ".  Will throw away current SAML ticket and try again.");
                context.getSsg().getTokenStrategy(SecurityTokenType.SAML_AUTHENTICATION).onTokenRejected();
                throw new PolicyRetryableException("Flushed rejected SAML ticket.");
            }
            // FALLTHROUGH: not handled by agent -- fall through and send it back to the client
        }

        ClientAssertion rootAssertion = appliedPolicy.getClientAssertion();
        if (rootAssertion != null) {
            AssertionStatus result = rootAssertion.unDecorateReply(context);
            if (result != AssertionStatus.NONE) {
                log.warning("Response policy processing failed with status " + result + "; aborting");
                throw new ConfigurationException("Unable to undecorate response; policy evaluated with error: " + result);
            }
        }
    }

    /**
     * Get the appropriate Ssg URL for forwarding the given request.
     *
     * @param context the context containing the request to process and the SSG it is bound for.  May not be null.
     * @return a URL that might have had it's protocol and port modified if SSL is indicated.  Never null.
     * @throws ConfigurationException if we couldn't find a valid URL in this Ssg configuration.
     */
    private URL getUrl(PolicyApplicationContext context) throws ConfigurationException {
        Ssg ssg = context.getSsg();
        URL url = null;
        try {
            url = ssg.getServerUrl();
            if (context.isSslRequired()) {
                if (context.isSslForbidden())
                    log.severe("Error: SSL is both forbidden and required by policy -- leaving SSL enabled");
                if ("http".equalsIgnoreCase(url.getProtocol())) {
                    log.info("Changing http to https per policy for this request (using SSL port " +
                      ssg.getSslPort() + ")");
                    url = new URL("https", url.getHost(), ssg.getSslPort(), url.getFile());
                } else
                    throw new ConfigurationException("Couldn't find an SSL-enabled version of protocol " +
                      url.getProtocol());
            }
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Client Proxy: Gateway \"" + ssg + "\" has an invalid server url: " +
              ssg.getServerUrl());
        }

        return url;
    }

    /**
     * Call the Ssg and obtain its response to the current message.
     *
     * @param context       the Context containing the request to process.
     *                      If a policy was applied, the context's activePolicy must point at it.
     * @throws ConfigurationException         if the SSG url is invalid
     * @throws ConfigurationException         if the we downloaded a new policy for this request, but we are still being told
     *                                        the policy is out-of-date
     * @throws ConfigurationException         if the SSG sends us an invalid Policy URL
     * @throws ConfigurationException         if the PendingRequest did not contain enough information to construct a
     *                                        valid PolicyAttachmentKey
     * @throws IOException                    if there was a network problem getting the message response from the SSG
     * @throws IOException                    if there was a network problem downloading a policy from the SSG
     * @throws PolicyRetryableException       if a new policy was downloaded
     * @throws ServerCertificateUntrustedException
     *                                        if a policy couldn't be downloaded because the SSG SSL certificate
     *                                        was not recognized and needs to be (re)imported
     * @throws OperationCanceledException     if credentials were needed to continue processing, but the user canceled
     *                                        the logon dialog (or we are running headless).
     * @throws ClientCertificateException     if our client cert is no longer valid, but we couldn't delete it from
     *                                        the keystore.
     * @throws BadCredentialsException        if the SSG rejected our SSG username and/or password.
     * @throws NoSuchAlgorithmException       if the client certificate key was not RSA
     * @throws InvalidDocumentFormatException if the response from the SSG was not a valid SOAP document
     * @throws com.l7tech.common.security.xml.processor.ProcessorException
     *                                        if the response from the SSG could not be undecorated
     */
    private void obtainResponse(final PolicyApplicationContext context)
      throws ConfigurationException, IOException, PolicyRetryableException, GeneralSecurityException,
      OperationCanceledException, ClientCertificateException, BadCredentialsException,
      KeyStoreCorruptException, HttpChallengeRequiredException, SAXException, NoSuchAlgorithmException,
      InvalidDocumentFormatException, ProcessorException, BadSecurityContextException
    {
        URL url = getUrl(context);
        final Ssg ssg = context.getSsg();

        HttpClient client = new HttpClient(context.getSsg().getHttpConnectionManager());
        HttpState state = client.getState();

        // Forget any cached session cookies, for all services shared by this SSG        
        if (context.isPolicyUpdated()) {
            ssg.clearSessionCookies();
        }

        Cookie[] cookies = context.getSsg().retrieveSessionCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                state.addCookie(cookie);
            }
        }

        PostMethod postMethod = null;
        final Message request = context.getRequest();
        final Message response = context.getResponse();

        try {
            postMethod = new PostMethod(url.toString());
            setAuthenticationAndBufferingState(context, state, postMethod);
            postMethod.addRequestHeader("SOAPAction", context.getPolicyAttachmentKey().getSoapAction());
            postMethod.addRequestHeader(SecureSpanConstants.HttpHeaders.ORIGINAL_URL, context.getOriginalUrl().toString());

            // Let the Gateway know what policy version we used for the request.
            Policy policy = context.getActivePolicy();
            if (policy != null && policy.getVersion() != null)
                postMethod.addRequestHeader(SecureSpanConstants.HttpHeaders.POLICY_VERSION, policy.getVersion());

            final Document decoratedDocument = request.getXmlKnob().getDocumentReadOnly();
            String postBody = XmlUtil.nodeToString(decoratedDocument);

            if (LogFlags.logPosts) {
                if (LogFlags.reformatLoggedXml) {
                    log.info("Posting to Gateway (reformatted):\n" +
                             XmlUtil.nodeToFormattedString(decoratedDocument));
                } else {
                    if (LogFlags.logAttachments && request.getMimeKnob().isMultipart()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        InputStream bodyStream = request.getMimeKnob().getEntireMessageBodyAsInputStream();
                        HexUtils.copyStream(bodyStream, baos);
                        log.info("Posting to Gateway (unformatted, including attachments):\n" +
                                 baos.toString(request.getMimeKnob().getOuterContentType().getEncoding()));
                    } else {
                        log.info("Posting to Gateway (unformatted):\n" + postBody);
                    }
                }
            }

            postMethod.addRequestHeader(MimeUtil.CONTENT_TYPE, request.getMimeKnob().getOuterContentType().getFullValue());

            final InputStream bodyInputStream = request.getMimeKnob().getEntireMessageBodyAsInputStream();
            postMethod.setRequestBody(bodyInputStream);

            log.info("Posting request to Gateway " + ssg + ", url " + url);
            CurrentRequest.setPeerSsg(ssg);
            int status = client.executeMethod(postMethod);
            CurrentRequest.setPeerSsg(null);
            log.info("POST to Gateway completed with HTTP status code " + status);

            Header certStatusHeader = postMethod.getResponseHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS);
            if (certStatusHeader != null && SecureSpanConstants.INVALID.equalsIgnoreCase(certStatusHeader.getValue())) {
                log.info("Gateway response contained a certficate status:invalid header.  Will get new client cert.");
                // Try to get a new client cert; if this succeeds, it'll replace the old one
                try {
                    if (context.getSsg().getTrustedGateway() != null) {
                        log.log(Level.SEVERE, "Federated Gateway " + context.getSsg() + " is trying to tell us to destroy our Trusted client certificate; ignoring it");
                        throw new ConfigurationException("Federated Gateway rejected our client certificate");
                    }
                    SsgKeyStoreManager.obtainClientCertificate(context.getSsg(), context.getCredentialsForTrustedSsg());
                    throw new PolicyRetryableException(); // try again with the new cert
                } catch (GeneralSecurityException e) {
                    throw new ClientCertificateException("Unable to obtain new client certificate", e);
                } catch (IOException e) {
                    throw new ClientCertificateException("Unable to obtain new client certificate", e);
                } catch (CertificateAlreadyIssuedException e) {
                    // Bug #380 - if we haven't updated policy yet, try that first - mlyons
                    if (!context.isPolicyUpdated()) {
                        ssg.rootPolicyManager().flushPolicy(context.getPolicyAttachmentKey());
                        throw new PolicyRetryableException();
                    } else {
                        Managers.getCredentialManager().notifyCertificateAlreadyIssued(ssg);
                        throw new CertificateAlreadyIssuedException(e);
                    }
                }
            }

            Header policyUrlHeader = postMethod.getResponseHeader(SecureSpanConstants.HttpHeaders.POLICYURL_HEADER);
            if (policyUrlHeader != null) {
                log.info("Gateway response contained a PolicyUrl header: " + policyUrlHeader.getValue());
                // Have we already updated a policy while processing this request?
                if (context.isPolicyUpdated())
                    throw new ConfigurationException("Policy was updated, but Gateway says it's still out-of-date");
                String serviceid = null;
                try {
                    URL policyUrl = new URL(policyUrlHeader.getValue());
                    // force the policy URL to point at the SSG hostname the user typed
                    policyUrl = new URL(policyUrl.getProtocol(), ssg.getSsgAddress(), policyUrl.getPort(), policyUrl.getFile());
                    String query = policyUrl.getQuery();
                    Matcher m = findServiceid.matcher(query);
                    if (m.matches())
                        serviceid = m.group(1);
                    if (serviceid == null || serviceid.length() < 1)
                        throw new ConfigurationException("Gateway sent us a Policy URL from which we were unable to extract the service ID.");

                } catch (MalformedURLException e) {
                    throw new ConfigurationException("Gateway sent us an invalid Policy URL.");
                }

                context.downloadPolicy(serviceid);
                context.setPolicyUpdated(true);
                if (status != 200) {
                    log.info("Retrying request with the new policy");
                    throw new PolicyRetryableException();
                }
                log.info("Will use new policy for future requests.");
            }

            HttpHeaders headers = new HttpHeaders(postMethod.getResponseHeaders());

            Header contentType = postMethod.getResponseHeader(MimeUtil.CONTENT_TYPE);
            log.info("Response Content-Type: " + contentType);
            if (contentType == null || contentType.getValue() == null) {
                log.warning("Server did not return a Content-Type");
                checkStatus(status, postMethod, url, ssg);
                throw new IOException("Response from Gateway did not include a Content-Type");
            }
            final ContentTypeHeader outerContentType = ContentTypeHeader.parseValue(contentType.getValue());
            if (!(outerContentType.isXml() || outerContentType.isMultipart())) {
                InputStream rStream = postMethod.getResponseBodyAsStream();
                byte[] output = null;
                if (rStream != null) {
                    output = HexUtils.slurpStream(rStream);
                }
                if (output != null) {
                    log.warning("Server returned unsupported Content-Type (" + outerContentType.getFullValue() +
                                ") with content\n" + new String(output));
                }
                checkStatus(status, postMethod, url, ssg);
                throw new IOException("Response from Gateway was unsupported Content-Type " + outerContentType.getFullValue());
            }


            InputStream responseBodyAsStream = postMethod.getResponseBodyAsStream();
            //responseBodyAsStream = new TeeInputStream(responseBodyAsStream, System.err);
            response.initialize(Managers.createStashManager(),
                                             outerContentType,
                                             responseBodyAsStream);
            response.attachHttpResponseKnob(new AbstractHttpResponseKnob() {
                public void addCookie(Cookie cookie) {
                    // Agent currently stores cookies in the Ssg instance, and does not pass them on to the client
                    throw new UnsupportedOperationException();
                }

                public void beginResponse() {
                    throw new UnsupportedOperationException();
                }
            });
            final PostMethod postMethod1 = postMethod;
            context.runOnClose(new Runnable() {
                public void run() {
                    postMethod1.releaseConnection();
                }
            });

            // Assert that response is XML
            Document responseDocument = response.getXmlKnob().getDocumentWritable();

            if (LogFlags.logResponse) {
                final MimeKnob respMime = response.getMimeKnob();
                if (LogFlags.reformatLoggedXml) {
                    String logStr = respMime.getOuterContentType().toString() + "\r\n" +
                            XmlUtil.nodeToFormattedString(responseDocument);
                    log.info("Got response from Gateway (reformatted):\n" + logStr);
                } else {
                    if (LogFlags.logAttachments && respMime.isMultipart()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        InputStream bodyStream = respMime.getEntireMessageBodyAsInputStream();
                        HexUtils.copyStream(bodyStream, baos);
                        log.info("Got response from Gateway (unformatted, including attachments):\n" +
                                 baos.toString(respMime.getOuterContentType().getEncoding()));
                    } else {
                        String logStr = respMime.getOuterContentType().toString() + "\r\n" +
                                XmlUtil.nodeToString(responseDocument);
                        log.info("Got response from Gateway (unformatted):\n" + logStr);
                    }
                }
            }

            log.info("Running SSG response through WS-Security undecorator");
            SecurityContextFinder scf = null;
            final String sessionId = context.getSecureConversationId();
            if (sessionId != null) {
                final byte[] sessionKey = context.getSecureConversationSharedSecret();
                scf = new SecurityContextFinder() {
                    public SecurityContext getSecurityContext(String securityContextIdentifier) {
                        return new SecurityContext() {
                            public SecretKey getSharedSecret() {
                                return new AesKey(sessionKey, sessionKey.length * 8);
                            }
                        };
                    }
                };
            }

            ProcessorResult processorResult = null;
            try {
                boolean haveKey = SsgKeyStoreManager.isClientCertUnlocked(ssg);
                final ProcessorResult processorResultRaw =
                  wssProcessor.undecorateMessage(responseDocument,
                    haveKey ? SsgKeyStoreManager.getClientCert(ssg) : null,
                    haveKey ? SsgKeyStoreManager.getClientCertPrivateKey(ssg) : null,
                    scf);
                // Translate timestamp in result from SSG time to local time
                final WssTimestamp wssTimestampRaw = processorResultRaw.getTimestamp();
                processorResult = new ProcessorResultWrapper(processorResultRaw) {
                    public WssTimestamp getTimestamp() {
                        return new WssTimestampWrapper(wssTimestampRaw, ssg.dateTranslatorFromSsg());
                    }
                };

            } catch (MessageNotSoapException e) {
                // Response is not SOAP.
                log.info("Response from Gateway is not SOAP.");
                processorResult = null;
            }

            response.attachKnob(HttpHeadersKnob.class, new HttpHeadersKnob(headers));
            response.getXmlKnob().setProcessorResult(processorResult);
            response.getHttpResponseKnob().setStatus(status);
            checkStatus(status, postMethod, url, ssg);

        } catch (NoSuchPartException e) {
            throw new CausedIOException("Response from Gateway used invalid or unsupported MIME multipart syntax", e);
        } finally {
            if (postMethod != null) {
                if (state.getCookies() == null) {
                    context.getSsg().clearSessionCookies();
                } else {
                    context.getSsg().storeSessionCookies(state.getCookies());
                }
            }
        }
    }

    /** Check for 401 or 402 status codes, and log and throw as appropriate. */
    private void checkStatus(int status, PostMethod postMethod, URL url, final Ssg ssg)
            throws KeyStoreCorruptException, BadCredentialsException
    {
        if (status == 401 || status == 402) {
            Header authHeader = postMethod.getResponseHeader("WWW-Authenticate");
            log.info("Got auth header: " + (authHeader == null ? "<null>" : authHeader.getValue()));
            if (authHeader == null && "https".equals(url.getProtocol()) && SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                log.info("Got 401 response from Gateway over https; possible that client cert is no good");
            }

            throw new BadCredentialsException();
        }
    }

    private static class LogFlags {
        private static final boolean logPosts = Boolean.getBoolean(PROPERTY_LOGPOSTS);
        private static final boolean logResponse = Boolean.getBoolean(PROPERTY_LOGRESPONSE);
        private static final boolean logAttachments = Boolean.getBoolean(PROPERTY_LOGATTACHMENTS);
        private static final boolean reformatLoggedXml = Boolean.getBoolean(PROPERTY_REFORMATLOGGEDXML);
        private static final boolean logPolicies = Boolean.getBoolean(PROPERTY_LOGPOLICIES);
    }

    /**
     * Configure HTTP Basic or Digest auth on the specified HttpState and PostMethod, if called for by
     * the specified PendingRequest, and set the request to be unbuffered if possible (ie, if Digest
     * is not required).
     *
     * @param context        the Context containing the request that might require HTTP level authentication
     * @param state      the HttpState to adjust
     * @param postMethod the PostMethod to adjust
     */
    private void setAuthenticationAndBufferingState(PolicyApplicationContext context, HttpState state, PostMethod postMethod)
            throws OperationCanceledException, IOException
    {
        // Turn off request buffering unless HTTP digest is required (Bug #1376)
        if (!context.isDigestAuthRequired()) {
            // Fix for Bug #1282 - Must set a content-length on PostMethod or it will try to buffer the whole thing
            final long contentLength = context.getRequest().getMimeKnob().getContentLength();
            if (contentLength > Integer.MAX_VALUE)
                throw new IOException("Body content is too long to be processed -- maximum is " + Integer.MAX_VALUE + " bytes");
            postMethod.setRequestContentLength((int)contentLength);
        }

        // Turn on preemptive authentication only if HTTP basic is called for in the policy
        state.setAuthenticationPreemptive(context.isBasicAuthRequired());

        // Do we need HTTP client auth?
        if (!context.isBasicAuthRequired() && !context.isDigestAuthRequired()) {
            // No -- tell the HTTP client to keep the password to itself (if it's caching one)
            log.info("No HTTP Basic or Digest authentication required by current policy");
            postMethod.setDoAuthentication(false);
            return;
        }

        if (context.getSsg().getTrustedGateway() != null) // can't happen; password based assertions should have all failed
            throw new OperationCanceledException("Password based authentication is not supported for Federated Gateway");

        // Set credentials and enable HTTP client auth
        String username = context.getUsername();
        char[] password = context.getPassword();
        log.info("Enabling HTTP Basic or Digest auth with user name=" + username);
        postMethod.setDoAuthentication(true);
        state.setCredentials(null, null,
                             new UsernamePasswordCredentials(username, new String(password)));
    }
}
