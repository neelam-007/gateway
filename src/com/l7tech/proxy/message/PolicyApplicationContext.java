/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.message;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.ProcessingContext;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.util.CausedIllegalStateException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.util.TokenServiceClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds message processing state needed by policy application point (SSB) message processor and policy assertions.
 */
public class PolicyApplicationContext extends ProcessingContext {

    private static final Logger logger = Logger.getLogger(PolicyApplicationContext.class.getName());
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final int SAML_PREEXPIRE_SEC = 30;
    private static final int WSSC_PREEXPIRE_SEC = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CredentialManager credentialManager = Managers.getCredentialManager();

    private final Document originalDoc;
    private final Ssg ssg;
    private final RequestInterceptor requestInterceptor;
    private final PolicyAttachmentKey policyAttachmentKey;
    private final URL originalUrl;
    private boolean isPolicyUpdated = false;
    private Long nonce = null; // nonce.  set on-demand, and only set once
    private String secureConversationId = null;
    private byte[] secureConversationSharedSecret = null;
    private SamlHolderOfKeyAssertion samlHolderOfKeyAssertion = null;
    private Calendar secureConversationExpiryDate = null;
    private ClientSidePolicy clientSidePolicy = ClientSidePolicy.getPolicy();

    // Policy settings, filled in by traversing policy tree, and which can all be rolled back by reset()
    private static class PolicySettings {
        private Policy activePolicy = null; // the policy that we most recently started applying to this request, if any
        private boolean isSslRequired = false;
        private boolean sslForbidden = false;  // ssl is forbidden for this request
        private boolean isBasicAuthRequired = false;
        private boolean isDigestAuthRequired = false;
        private DecorationRequirements wssRequirements = new DecorationRequirements();
        private String messageId = null;
        private Map pendingDecorations = new LinkedHashMap();
    }    
    
    private PolicySettings policySettings = new PolicySettings();
    
    /**
     * Create a new policy application context.  This holds information not specific to the request or response
     * message -- which is in the request and response -- but that will be needed by the policy application
     * assertions.
     *  
     * @param ssg                the Ssg to which this request is destined.  Must not be null.
     * @param request   the request that is to be decorated.  Must not be null.
     * @param response  a Message to hold the response (before a response is obtained, for 
     *                  {@link com.l7tech.proxy.policy.assertion.ClientDecorator#decorateRequest}) 
     *                  or which contains the 
     *                  response (after the response is obtained, for
     *                  {@link com.l7tech.proxy.policy.assertion.ClientAssertion#unDecorateReply}).
     *                  Must not be null. 
     * @param requestInterceptor a RequestInterceptor that wishes to be notified about policy updates, or null.
     * @param policyAttachmentKey the soapaction, namespace, and uri that apply to this request, or null
     * @param origUrl            the reconstructed local URL from which this request arrived, or null
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IllegalStateException if this Message has not yet been attached to an InputStream.
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException       if originalDocument needs to be serialized, but cannot be, due to some
     *                           canonicalizer problem (relative namespaces, maybe)
     */
    public PolicyApplicationContext(Ssg ssg, Message request, Message response,
                                    RequestInterceptor requestInterceptor,
                                    PolicyAttachmentKey policyAttachmentKey,
                                    URL origUrl)
            throws SAXException, IOException
    {
        super(request, response);
        originalDoc = request.getXmlKnob().getDocument();
        if (ssg == null) throw new NullPointerException("ssg is null");
        if (requestInterceptor == null)
            requestInterceptor = NullRequestInterceptor.INSTANCE;
        if (origUrl == null)
            origUrl = makeFakeOriginalUrl();
        if (policyAttachmentKey == null)
            policyAttachmentKey = new PolicyAttachmentKey(null, null, null);
        this.originalUrl = origUrl;
        this.ssg = ssg;
        this.requestInterceptor = requestInterceptor;
        this.policyAttachmentKey = policyAttachmentKey;
    }
    
    private URL makeFakeOriginalUrl() {
        URL origUrl;
        try {
           origUrl = new URL("http://localhost:7700/nourl");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // can't happen
        }
        return origUrl;
    }

    /**
     * Reset all policy settings in preperation for starting processing over again with a different policy.
     */
    public void reset() {
        try {
            getRequest().getXmlKnob().setDocument(originalDoc);
        } catch (SAXException e) {
            throw new CausedIllegalStateException(e); // can't happen, it's always got an XML knob
        }
        policySettings = new PolicySettings();
    }

    public Document getOriginalDocument() {
        return originalDoc;
    }

    public Ssg getSsg() {
        return ssg;
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

    /**
     * @return the original url, or null if there wasn't one.
     */
    public URL getOriginalUrl() {
        return originalUrl;
    }

    public PolicyAttachmentKey getPolicyAttachmentKey() {
        return policyAttachmentKey;
    }

    public void setSslForbidden(boolean sslForbidden) {
        this.policySettings.sslForbidden = sslForbidden;
    }

    public boolean isSslForbidden() {
        return this.policySettings.sslForbidden;
    }

    public ClientSidePolicy getClientSidePolicy() {
        return clientSidePolicy;
    }

    /**
     * @return the policy we are currently applying to this request, or null if we don't know or don't have one.
     */
    public Policy getActivePolicy() {
        return this.policySettings.activePolicy;
    }

    /**
     * Set the policy we are going to apply to this request.
     */
    public void setActivePolicy(Policy policy) {
        this.policySettings.activePolicy = policy;
    }

    public DecorationRequirements getWssRequirements() {
        return policySettings.wssRequirements;
    }

    /**
     * @return the Map of (assertion instance => ClientDecorator), containing deferred decorations to apply.
     */
    public Map getPendingDecorations() {
        return policySettings.pendingDecorations;
    }

    public String getL7aMessageId() {
        return policySettings.messageId;
    }

    public void setL7aMessageId(String newId) {
        policySettings.messageId = newId;
    }

    public String getUsername() throws OperationCanceledException {
        return getCredentialsForTrustedSsg().getUserName();
    }

    public char[] getPassword() throws OperationCanceledException {
        return getCredentialsForTrustedSsg().getPassword();
    }

    public long getNonce() {
        if (nonce == null)
            nonce = new Long(SECURE_RANDOM.nextLong());
        return nonce.longValue();
    }

    /**//**//*//**//*//**//*//**//*//**//*//**//*//**//*//**//*//**//*//**/
    /**//**//*   BEGIN STUPID SHIT   TODO: move this elsewhere   *//**//**/
    /**//**//*//**//*//**//*//**//*//**//*//**//*//**//*//**//*//**//*//**/

    /**
     * Assert that credentials must be available to continue processing this request, and return the credentials.
     * The user will be prompted for credentials if necessary.
     * This method does not affect the subsequent behaviour of getUsername() and getPassword().
     * <p/>
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
        PasswordAuthentication pw = getPasswordAuthentication();
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1 || pw.getPassword() == null)
            pw = credentialManager.getCredentials(trusted);
        return pw;
    }

    /**
     * Get the login credentials.  Should not be used by Agent code.
     *
     * @deprecated Agent code should use getCredentialsForTrustedSsg() or getFederatedCredentials() instead.
     */
    public LoginCredentials getCredentials() {
        throw new UnsupportedOperationException(); // TODO fix this LSP violation
    }

    private PasswordAuthentication getPasswordAuthentication() {
        LoginCredentials lc = super.getCredentials();
        if (lc == null) return null;
        PasswordAuthentication pw = new PasswordAuthentication(lc.getLogin(), lc.getCredentials());
        return pw;
    }

    /**
     * Assert that credentials must be available to continue processing this request,
     * but that the existing ones were found to be no good.  Other than first throwing out
     * any existing configured credentials and displaying an error message,
     * this behaves like gatherCredentials().
     */
    public PasswordAuthentication getNewCredentials() throws OperationCanceledException, HttpChallengeRequiredException {
        if (ssg.isChainCredentialsFromClient())
            throw new HttpChallengeRequiredException("Invalid user name or password");
        PasswordAuthentication pw = credentialManager.getNewCredentials(ssg, true);
        setCredentials(new LoginCredentials(pw.getUserName(), pw.getPassword(), CredentialFormat.CLEARTEXT, null));
        return pw;
    }

    /**
     * Assert that credentials must be available to continue processing this request, and return the credentials.
     * The user will be prompted for credentials if necessary.
     * If this method returns, getUsername() and getPassword() are guaranteed to return non-null
     * values for the rest of the lifetime of this request.
     * <p/>
     * This method may not be used if the current SSG is federated.
     *
     * @throws OperationCanceledException if credentials are not available, and the CredentialManager
     *                                    was unable to get some, possibly because the user canceled
     *                                    the logon dialog.
     */
    public PasswordAuthentication getCredentialsForTrustedSsg() throws OperationCanceledException {
        final Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            throw new OperationCanceledException("Not permitted to send real password to Federated Gateway.");
        PasswordAuthentication pw = getPasswordAuthentication();
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1 || pw.getPassword() == null)
            pw = credentialManager.getCredentials(ssg);
        return pw;
    }

    /**
     * Ensure that a client certificate is available for the current request.  Will apply for one
     * if necessary.
     * <p/>
     * This will work for both Trusted and Federated Gateways.  In the Federated case, the cert
     * will be applied for through the Trusted gateway.
     */
    public void prepareClientCertificate() throws OperationCanceledException, KeyStoreCorruptException,
      GeneralSecurityException, ClientCertificateException,
      ServerCertificateUntrustedException, BadCredentialsException, PolicyRetryableException {
        try {
            if (!SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                logger.info("PendingRequest: applying for client certificate");
                Ssg trusted = ssg.getTrustedGateway();
                if (trusted != null) {
                    SsgKeyStoreManager.obtainClientCertificate(trusted, getFederatedCredentials());
                } else {
                    SsgKeyStoreManager.obtainClientCertificate(ssg, getCredentialsForTrustedSsg());
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

    /**
     * Ensure that there is a Wsa message ID in this request.
     */
    public void prepareWsaMessageId() throws InvalidDocumentFormatException {
        if (getL7aMessageId() == null) {
            String id = SoapUtil.getL7aMessageId(getOriginalDocument());

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
      IOException {
        prepareClientCertificate();
        Ssg ssg = getSsg();
        logger.log(Level.INFO, "Establishing new WS-SecureConversation session with Gateway " + ssg.toString());
        TokenServiceClient.SecureConversationSession s =
          TokenServiceClient.obtainSecureConversationSession(ssg,
            SsgKeyStoreManager.getClientCert(ssg),
            SsgKeyStoreManager.getClientCertPrivateKey(ssg),
            SsgKeyStoreManager.getServerCert(ssg));
        logger.log(Level.INFO, "WS-SecureConversation session established with Gateway " + ssg.toString() + "; session ID=" + s.getSessionId());
        ssg.secureConversationId(s.getSessionId());
        secureConversationId = s.getSessionId();
        ssg.secureConversationSharedSecret(s.getSharedSecret());
        secureConversationSharedSecret = s.getSharedSecret();
        if (s.getExpiryDate() == null) {
            logger.info("WS-SecureConversation session did not include an expiry date.  Assuming expiry 600 seconds from now.");
            Calendar expiry = Calendar.getInstance(UTC_TIME_ZONE);
            expiry.add(Calendar.SECOND, WSSC_PREEXPIRE_SEC);
            secureConversationExpiryDate = expiry;
            ssg.secureConversationExpiryDate(expiry);
        } else {
            Calendar expiry = Calendar.getInstance(UTC_TIME_ZONE);
            expiry.setTime(ssg.dateTranslatorFromSsg().translate(s.getExpiryDate()));
            secureConversationExpiryDate = expiry;
            ssg.secureConversationExpiryDate(expiry);
            Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
            now.add(Calendar.SECOND, WSSC_PREEXPIRE_SEC);
            if (!expiry.after(now))
                logger.warning("Significant clock skew detected between local machine (currently " + now + ") and Gateway " + ssg + " (token expiry " + expiry + ").  WS-SecureConversation sessions will expire after every message.");
            else
                logger.info("WS-SecureConversation session will expire in " + Math.floor((expiry.getTime().getTime() - now.getTime().getTime()) / 1000) + " sec");
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
                        logger.log(Level.INFO, "Our WS-SecureConversation session has expired or will do so within the next " +
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
     *
     * @return The secure conversation session ID for this session, or null if there isn't one.
     */
    public String getSecureConversationId() {
        return secureConversationId;
    }

    /**
     * Get the secure conversation ID used for this request.
     * Establishes a new session with the SSG if necessary.
     *
     * @return the secure conversation ID for this session, which may be newly created.  Never null.
     */
    public String getOrCreateSecureConversationId()
      throws OperationCanceledException, GeneralSecurityException, IOException, KeyStoreCorruptException,
      ClientCertificateException, BadCredentialsException, PolicyRetryableException {
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
     * Forget any currently-active SecureConversationSession with this SSG.
     */
    public void closeSecureConversationSession() {
        synchronized (ssg) {
            secureConversationId = null;
            secureConversationExpiryDate = null;
            secureConversationSharedSecret = null;
            ssg.secureConversationId(null);
            ssg.secureConversationExpiryDate(null);
            ssg.secureConversationSharedSecret(null);
        }
    }

    /**
     * Get the secure conversation shared secret used for this request.
     *
     * @return the secure conversation shared secret for this session, or null if there isn't one.
     */
    public byte[] getSecureConversationSharedSecret() {
        if (secureConversationSharedSecret != null)
            return secureConversationSharedSecret;
        return secureConversationSharedSecret = ssg.secureConversationSharedSecret();
    }

    /**
     * Check the expiry date of our hok, and throw it away if it has started to smell a bit off.
     */
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
                        logger.log(Level.INFO, "Our SAML Holder-of-key assertion has expired or will do so within the next " +
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
     * @throws OperationCanceledException if the user cancels the login dialog
     * @throws GeneralSecurityException   if there is a problem with a certificate, key, or signature
     * @throws IOException                if there is a problem reading from the network or a file
     * @throws KeyStoreCorruptException   if our local key store or trust store is damaged
     * @throws ClientCertificateException if we need a client certificate but can't obtain one
     * @throws BadCredentialsException    if we need a certificate but our username and password is wrong
     * @throws PolicyRetryableException   if we should retry policy processing from the beginning
     */
    public SamlHolderOfKeyAssertion getOrCreateSamlHolderOfKeyAssertion()
      throws OperationCanceledException, GeneralSecurityException, IOException, KeyStoreCorruptException,
      ClientCertificateException, BadCredentialsException, PolicyRetryableException {
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
     *
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
      KeyStoreCorruptException, PolicyRetryableException, BadCredentialsException, IOException {
        prepareClientCertificate();
        Ssg ssg = getSsg();
        Ssg tokenServerSsg = ssg.getTrustedGateway();
        if (tokenServerSsg == null) tokenServerSsg = ssg;
        logger.log(Level.INFO, "Applying for SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
        SamlHolderOfKeyAssertion s =
          TokenServiceClient.obtainSamlHolderOfKeyAssertion(tokenServerSsg,
            SsgKeyStoreManager.getClientCert(tokenServerSsg),
            SsgKeyStoreManager.getClientCertPrivateKey(tokenServerSsg),
            SsgKeyStoreManager.getServerCert(tokenServerSsg));
        logger.log(Level.INFO, "Obtained SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
        ssg.samlHolderOfKeyAssertion(s);
        samlHolderOfKeyAssertion = s;
        return samlHolderOfKeyAssertion;
    }

}
