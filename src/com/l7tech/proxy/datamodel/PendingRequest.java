/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.util.TokenServiceClient;
import com.l7tech.message.MultipartMessageReader;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds request state while the client proxy is processing it.
 * User: mike
 * Date: Jun 16, 2003
 * Time: 12:04:09 PM
 */
public class PendingRequest {
    private static final Logger log = Logger.getLogger(PendingRequest.class.getName());
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final int SAML_PREEXPIRE_SEC = 30;
    private static final int WSSC_PREEXPIRE_SEC = 30;

    //private ClientProxy clientProxy;
    private final CredentialManager credentialManager = Managers.getCredentialManager();
    private Document soapEnvelope;
    private Document initialEnvelope;
    private final Ssg ssg;
    private final RequestInterceptor requestInterceptor;
    private String soapAction = "";
    private String uri = "";
    private SsgResponse lastErrorResponse = null; // Last response received from SSG in the case of 401 or 500 status
    private boolean isPolicyUpdated = false;
    private URL originalUrl = null;
    private Long nonce = null; // nonce.  set on-demand, and only set once
    private HttpHeaders headers = null;
    private PasswordAuthentication pw = null;
    private ClientSidePolicy clientSidePolicy = ClientSidePolicy.getPolicy();
    private String secureConversationId = null;
    private byte[] secureConversationSharedSecret = null;
    private SamlHolderOfKeyAssertion samlHolderOfKeyAssertion = null;
    private Calendar secureConversationExpiryDate = null;
    private boolean multipart = false;
    private MultipartMessageReader multipartReader = null;

    // Policy settings, filled in by traversing policy tree
    private static class PolicySettings {
        private Document decoratedSoapEnvelope = null;
        private Policy activePolicy= null; // the policy that we most recently started applying to this request, if any
        private boolean isSslRequired = false;
        private boolean sslForbidden = false;  // ssl is forbidden for this request
        private boolean isBasicAuthRequired = false;
        private boolean isDigestAuthRequired = false;
        private WssDecorator.DecorationRequirements wssRequirements = new WssDecorator.DecorationRequirements();
        private String messageId = null;
        private Map pendingDecorations = new LinkedHashMap();
    }
    private PolicySettings policySettings = new PolicySettings();

    /** Construct a PendingRequest around the given SOAPEnvelope going to the given SSG. */
    public PendingRequest(Document soapEnvelope, Ssg ssg, RequestInterceptor requestInterceptor) {
        this.soapEnvelope = soapEnvelope;
        this.initialEnvelope = soapEnvelope;
        this.ssg = ssg;
        this.requestInterceptor = requestInterceptor;
    }

    public PendingRequest(Document soapEnvelope, Ssg ssg, RequestInterceptor ri, URL origUrl, HttpHeaders headers) {
        this(soapEnvelope, ssg, ri);
        this.originalUrl = origUrl;
        this.headers = headers;
    }

    /**
     * Reset all policy settings in preperation for starting processing over again with a different policy.
     */
    public void reset() {
        policySettings = new PolicySettings();
        soapEnvelope = initialEnvelope;
    }

    /**
     * Ensure that a client certificate is available for the current request.  Will apply for one
     * if necessary.
     * <p>
     * This will work for both Trusted and Federated Gateways.  In the Federated case, the cert
     * will be applied for through the Trusted gateway.
     *
     */
    public void prepareClientCertificate() throws OperationCanceledException, KeyStoreCorruptException,
            GeneralSecurityException, ClientCertificateException,
            ServerCertificateUntrustedException, BadCredentialsException, PolicyRetryableException
    {
        try {
            if (!SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                log.info("PendingRequest: applying for client certificate");
                Ssg trusted = ssg.getTrustedGateway();
                if (trusted != null) {
                    SsgKeyStoreManager.obtainClientCertificate(trusted, getFederatedCredentials());
                } else {
                    SsgKeyStoreManager.obtainClientCertificate(ssg, getCredentials());
                }
            }
        } catch (CertificateAlreadyIssuedException e) {
            // Bug #380 - if we haven't updated policy yet, try that first - mlyons
            if (!isPolicyUpdated()) {
                Managers.getPolicyManager().flushPolicy(this);
                throw new PolicyRetryableException();
            } else {
                Managers.getCredentialManager().notifyCertificateAlreadyIssued(ssg);
                throw new OperationCanceledException("Unable to obtain a client certificate");
            }
        } catch (IOException e) {
            throw new ClientCertificateException("Unable to obtain a client certificate", e);
        }
    }

    /** Manually set the credentials to use throughout this request. */
    public void setCredentials(PasswordAuthentication pw) {
        this.pw = pw;
    }

    /**
     * Assert that credentials must be available to continue processing this request,
     * but that the existing ones were found to be no good.  Other than first throwing out
     * any existing configured credentials and displaying an error message,
     * this behaves like gatherCredentials().
     */
    public PasswordAuthentication getNewCredentials() throws OperationCanceledException, HttpChallengeRequiredException {
        if (ssg.isChainCredentialsFromClient())
            throw new HttpChallengeRequiredException("Invalid username or password");
        return this.pw = credentialManager.getNewCredentials(ssg);
    }

    /**
     * Assert that credentials must be available to continue processing this request, and return the credentials.
     * The user will be prompted for credentials if necessary.
     * If this method returns, getUsername() and getPassword() are guaranteed to return non-null
     * values for the rest of the lifetime of this request.
     * <p>
     * This method may not be used if the current SSG is federated.
     *
     * @throws OperationCanceledException if credentials are not available, and the CredentialManager
     *                                    was unable to get some, possibly because the user canceled
     *                                    the logon dialog.
     */
    public PasswordAuthentication getCredentials() throws OperationCanceledException {
        final Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            throw new OperationCanceledException("Not permitted to send real password to Federated Gateway.");
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1 || pw.getPassword() == null)
            pw = credentialManager.getCredentials(ssg);
        return pw;
    }

    /**
     * Assert that credentials must be available to continue processing this request, and return the credentials.
     * The user will be prompted for credentials if necessary.
     * This method does not affect the subsequent behaviour of getUsername() and getPassword().
     * <p>
     * This method may be only be used if the current SSG is federated.
     * This method returns the credentials of the Trusted Gateway, and the caller is responsible for ensuring
     * that these credentials are not exposed to the Federated Gateway at any time.
     *
     * @throws OperationCanceledException if credentials are not available, and the CredentialManager
     *                                    was unable to get some, possibly because the user canceled
     *                                    the logon dialog.
     */
    public PasswordAuthentication getFederatedCredentials()
            throws OperationCanceledException
    {
        final Ssg trusted = ssg.getTrustedGateway();
        if (trusted == null)
            throw new OperationCanceledException("Trusted Gateway does not have any Federated credentials");
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1 || pw.getPassword() == null)
            pw = credentialManager.getCredentials(trusted);
        return pw;
    }

    public String getUsername() throws OperationCanceledException {
        return getCredentials().getUserName();
    }

    public char[] getPassword() throws OperationCanceledException {
        return getCredentials().getPassword();
    }

    public long getNonce() {
        if (nonce == null)
            nonce = new Long(new SecureRandom().nextLong());
        return nonce.longValue();
   }

    // Getters and setters

    /**
     * Get the working copy of the Document representing the request.  This returns a copy that can be
     * modified freely.
     *
     * If you want your changes to stick, you'll need to save them back by calling setSoapEnvelope().
     *
     * @return A copy of the SOAP envelope Document, which may be freely modified.
     */
    public Document getDecoratedSoapEnvelope() {
        if (policySettings.decoratedSoapEnvelope != null)
            return policySettings.decoratedSoapEnvelope;
        return policySettings.decoratedSoapEnvelope = (Document) soapEnvelope.cloneNode(true);
    }

    /**
     * Get the actual Document representing the request, which should not be modified.  Any change
     * to this Document will prevent the reset() method from returning this PendingRequest to
     * its original state.  If you need to change the Document, use getDecoratedSoapEnvelope() instead.
     *
     * @return A reference to the original SOAP envelope Document, which must not be modified in any way.
     */
    public Document getUndecoratedSoapEnvelope() {
        return soapEnvelope;
    }

    /**
     * Replace the active Soap Envelope with a new one.  The original envelope is in any case kept
     * squirreled away so that reset() will work.
     * @param newEnvelope
     */
    public void setSoapEnvelope(Document newEnvelope) {
        soapEnvelope = newEnvelope;
    }

    public Ssg getSsg() {
        return ssg;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    public MultipartMessageReader getMultipartReader() {
        return multipartReader;
    }

    public void setMultipartReader(MultipartMessageReader multipartReader) {
        this.multipartReader = multipartReader;
    }

    public RequestInterceptor getRequestInterceptor() {
        return requestInterceptor;
    }

    public boolean isSslRequired() {
        return policySettings.isSslRequired;
    }

    public void setSslRequired(boolean sslRequired) {
        policySettings.isSslRequired = sslRequired;
    }

    public boolean isBasicAuthRequired() {
        return policySettings.isBasicAuthRequired;
    }

    public void setBasicAuthRequired(boolean basicAuthRequired) {
        policySettings.isBasicAuthRequired = basicAuthRequired;
    }

    public boolean isDigestAuthRequired() {
        return policySettings.isDigestAuthRequired;
    }

    public void setDigestAuthRequired(boolean digestAuthRequired) {
        policySettings.isDigestAuthRequired = digestAuthRequired;
    }

    public boolean isPolicyUpdated() {
        return isPolicyUpdated;
    }

    public void setPolicyUpdated(boolean policyUpdated) {
        isPolicyUpdated = policyUpdated;
    }

    public SsgResponse getLastErrorResponse() {
        return lastErrorResponse;
    }

    public void setLastErrorResponse(SsgResponse lastErrorResponse) {
        this.lastErrorResponse = lastErrorResponse;
    }

    public URL getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(URL originalUrl) {
        this.originalUrl = originalUrl;
    }

    public void setSslForbidden(boolean sslForbidden) {
        this.policySettings.sslForbidden = sslForbidden;
    }

    public boolean isSslForbidden() {
        return this.policySettings.sslForbidden;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public ClientSidePolicy getClientSidePolicy() {
        return clientSidePolicy;
    }

    /** @return the policy we are currently applying to this request, or null if we don't know or don't have one. */
    public Policy getActivePolicy() {
        return this.policySettings.activePolicy;
    }

    /** Set the policy we are going to apply to this request. */
    public void setActivePolicy(Policy policy) {
        this.policySettings.activePolicy = policy;
    }

    public WssDecorator.DecorationRequirements getWssRequirements() {
        return policySettings.wssRequirements;
    }

    /** @return the Map of (assertion instance => ClientDecorator), containing deferred decorations to apply. */
    public Map getPendingDecorations() {
        return policySettings.pendingDecorations;
    }

    public String getL7aMessageId() {
        return policySettings.messageId;
    }

    public void setL7aMessageId(String newId) {
        policySettings.messageId = newId;
    }

    /** Ensure that there is a Wsa message ID in this request. */
    public void prepareWsaMessageId() throws InvalidDocumentFormatException {
        if (getL7aMessageId() == null) {
            String id = SoapUtil.getL7aMessageId(getUndecoratedSoapEnvelope());

            if (id == null) {
                id = SoapUtil.generateUniqeUri();
            } else if (id.trim().length() < 1)
                throw new InvalidDocumentFormatException("Request has existing L7a:MessageID field that is empty or contains only whitespace");

            setL7aMessageId(id);
        }
    }

    private String establishSecureConversationSession()
            throws OperationCanceledException, GeneralSecurityException, ClientCertificateException,
            KeyStoreCorruptException, PolicyRetryableException, BadCredentialsException,
            IOException
    {
        prepareClientCertificate();
        Ssg ssg = getSsg();
        log.log(Level.INFO, "Establishing new WS-SecureConversation session with Gateway " + ssg.toString());
        TokenServiceClient.SecureConversationSession s =
                TokenServiceClient.obtainSecureConversationSession(ssg,
                                                                   SsgKeyStoreManager.getClientCert(ssg),
                                                                   SsgKeyStoreManager.getClientCertPrivateKey(ssg),
                                                                   SsgKeyStoreManager.getServerCert(ssg));
        log.log(Level.INFO, "WS-SecureConversation session established with Gateway " + ssg.toString() + "; session ID=" + s.getSessionId());
        ssg.secureConversationId(s.getSessionId());
        secureConversationId = s.getSessionId();
        ssg.secureConversationSharedSecret(s.getSharedSecret());
        secureConversationSharedSecret = s.getSharedSecret();
        if (s.getExpiryDate() == null) {
            log.info("WS-SecureConversation session did not include an expiry date.  Assuming expiry 600 seconds from now.");
            Calendar expiry = Calendar.getInstance(UTC_TIME_ZONE);
            expiry.add(Calendar.SECOND, WSSC_PREEXPIRE_SEC);
            secureConversationExpiryDate = expiry;
            ssg.secureConversationExpiryDate(expiry);
        } else {
            Calendar expiry = Calendar.getInstance(UTC_TIME_ZONE);
            expiry.setTime(s.getExpiryDate());
            secureConversationExpiryDate = expiry;
            ssg.secureConversationExpiryDate(expiry);
            Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
            now.add(Calendar.SECOND, WSSC_PREEXPIRE_SEC);
            if (!expiry.after(now))
                log.warning("Significant clock skew detected between local machine (currently " + now + ") and Gateway " + ssg + " (token expiry " + expiry + ").  WS-SecureConversation sessions will expire after every message.");
            else
                log.info("WS-SecureConversation session will expire in " + Math.floor((expiry.getTime().getTime() - now.getTime().getTime())/1000) + " sec");
        }
        return secureConversationId;
    }

    private void checkExpiredSecureConversationSession() {
        Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
        now.add(Calendar.SECOND, WSSC_PREEXPIRE_SEC);
        if (secureConversationExpiryDate != null) {
            if (!secureConversationExpiryDate.after(now)) {
                // See if we need to throw out the one cached in the Ssg object as well
                synchronized (ssg) {
                    Calendar ssgDate = ssg.secureConversationExpiryDate();
                    if (ssgDate == secureConversationExpiryDate || (ssgDate != null && !ssgDate.after(now))) {
                        log.log(Level.INFO, "Our WS-SecureConversation session has expired or will do so within the next " +
                                            WSSC_PREEXPIRE_SEC + "seconds.  Will throw it away and get a new one.");
                        ssg.secureConversationId(null);
                        ssg.secureConversationSharedSecret(null);
                        ssg.secureConversationExpiryDate(null);
                    }
                }
            }

            secureConversationId = null;
            secureConversationSharedSecret = null;
            secureConversationExpiryDate = null;
        }
    }

    /**
     * Get the secure conversation ID.  This returns the ID that was used in this request, if any.  No checking
     * of session expiry is done by this method.
     * @return The secure conversation session ID for this session, or null if there isn't one.
     */
    public String getSecureConversationId() {
        if (secureConversationId != null) {
            return secureConversationId;
        }
        secureConversationId = ssg.secureConversationId();
        return secureConversationId;
    }

    /**
     * Get the secure conversation ID used for this request.
     * Establishes a new session with the SSG if necessary.
     * @return the secure conversation ID for this session, which may be newly created.  Never null.
     */
    public String getOrCreateSecureConversationId()
            throws OperationCanceledException, GeneralSecurityException, IOException, KeyStoreCorruptException,
                   ClientCertificateException, BadCredentialsException, PolicyRetryableException
    {
        checkExpiredSecureConversationSession();
        if (secureConversationId != null)
          return secureConversationId;

        secureConversationId = ssg.secureConversationId();
        checkExpiredSecureConversationSession();
        if (secureConversationId != null)
            return secureConversationId;

        return secureConversationId = establishSecureConversationSession();
    }

    /**
     * Get the secure conversation shared secret used for this request.
     * @return the secure conversation shared secret for this session, or null if there isn't one.
     */
    public byte[] getSecureConversationSharedSecret()
    {
        if (secureConversationSharedSecret != null)
            return secureConversationSharedSecret;
        return secureConversationSharedSecret = ssg.secureConversationSharedSecret();
    }

    /** Check the expiry date of our hok, and throw it away if it has started to smell a bit off. */
    private void checkExpiredHolderOfKeyAssertion() {
        if (samlHolderOfKeyAssertion != null) {
            Calendar expires = samlHolderOfKeyAssertion.getExpires();
            Calendar nowUtc = Calendar.getInstance(UTC_TIME_ZONE);
            nowUtc.add(Calendar.SECOND, SAML_PREEXPIRE_SEC);
            if (!expires.after(nowUtc)) {
                // See if we need to throw out the one cached in the Ssg as well
                synchronized (ssg) {
                    SamlHolderOfKeyAssertion ssgHok = ssg.samlHolderOfKeyAssertion();
                    if (ssgHok == samlHolderOfKeyAssertion || (ssgHok != null && !ssgHok.getExpires().after(nowUtc))) {
                        log.log(Level.INFO, "Our SAML Holder-of-key assertion has expired or will do so within the next " +
                                            SAML_PREEXPIRE_SEC + " seconds.  Will throw it away and get a new one.");
                        ssg.samlHolderOfKeyAssertion(null);
                    }
                }

                samlHolderOfKeyAssertion = null;
            }
        }
    }

    /**
     * Get a valid holder-of-key SAML assertion for this SSG (or the Trusted SSG if this is a federated SSG).
     * If we don't currently hold a valid holder-of-key SAML assertion we will apply for a new one.
     *
     * @return A SAML assertion with us as the subject and our trusted SSG as the issuer.  Never null.
     * @throws OperationCanceledException   if the user cancels the login dialog
     * @throws GeneralSecurityException     if there is a problem with a certificate, key, or signature
     * @throws IOException                  if there is a problem reading from the network or a file
     * @throws KeyStoreCorruptException     if our local key store or trust store is damaged
     * @throws ClientCertificateException   if we need a client certificate but can't obtain one
     * @throws BadCredentialsException      if we need a certificate but our username and password is wrong
     * @throws PolicyRetryableException     if we should retry policy processing from the beginning
     */
    public SamlHolderOfKeyAssertion getOrCreateSamlHolderOfKeyAssertion()
            throws OperationCanceledException, GeneralSecurityException, IOException, KeyStoreCorruptException,
                   ClientCertificateException, BadCredentialsException, PolicyRetryableException
    {
        checkExpiredHolderOfKeyAssertion();
        if (samlHolderOfKeyAssertion != null)
            return samlHolderOfKeyAssertion;

        samlHolderOfKeyAssertion = ssg.samlHolderOfKeyAssertion();
        checkExpiredHolderOfKeyAssertion();
        if (samlHolderOfKeyAssertion != null)
            return samlHolderOfKeyAssertion;

        return samlHolderOfKeyAssertion = acquireSamlHolderOfKeyAssertion();
    }

    /**
     * Get our SAML holder-of-key assertion for the current SSG (or Trusted SSG).
     * @return our currently valid SAML holder-of-key assertion or null if we don't have one.
     */
    public SamlHolderOfKeyAssertion getSamlHolderOfKeyAssertion() {
        if (samlHolderOfKeyAssertion != null) {
            checkExpiredHolderOfKeyAssertion();
            return samlHolderOfKeyAssertion;
        }
        samlHolderOfKeyAssertion = ssg.samlHolderOfKeyAssertion();
        checkExpiredHolderOfKeyAssertion();
        return samlHolderOfKeyAssertion;
    }

    private SamlHolderOfKeyAssertion acquireSamlHolderOfKeyAssertion()
            throws OperationCanceledException, GeneralSecurityException, ClientCertificateException,
                   KeyStoreCorruptException, PolicyRetryableException, BadCredentialsException, IOException
    {
        prepareClientCertificate();
        Ssg ssg = getSsg();
        Ssg tokenServerSsg = ssg.getTrustedGateway();
        if (tokenServerSsg == null) tokenServerSsg = ssg;
        log.log(Level.INFO, "Applying for SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
        SamlHolderOfKeyAssertion s =
                TokenServiceClient.obtainSamlHolderOfKeyAssertion(tokenServerSsg,
                                                                   SsgKeyStoreManager.getClientCert(tokenServerSsg),
                                                                   SsgKeyStoreManager.getClientCertPrivateKey(tokenServerSsg),
                                                                   SsgKeyStoreManager.getServerCert(tokenServerSsg));
        log.log(Level.INFO, "Obtained SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
        ssg.samlHolderOfKeyAssertion(s);
        samlHolderOfKeyAssertion = s;
        return samlHolderOfKeyAssertion;
    }
}
