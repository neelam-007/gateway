/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.proxy.message;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.message.Message;
import com.l7tech.message.ProcessingContext;
import com.l7tech.message.SecurityKnob;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.security.socket.LocalTcpPeerIdentifier;
import com.l7tech.security.socket.LocalTcpPeerIdentifierFactory;
import com.l7tech.security.token.HasUsername;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.wstrust.TokenServiceClient;
import com.l7tech.security.wstrust.WsTrustConfig;
import com.l7tech.security.wstrust.WsTrustConfigFactory;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds message processing state needed by policy application point (SSB) message processor and policy assertions.
 * This class is not intended to be used across multiple threads (nor would it make sense if it were).
 */
@SuppressWarnings({"JavaDoc"})
public class PolicyApplicationContext extends ProcessingContext {

    private static final Logger logger = Logger.getLogger(PolicyApplicationContext.class.getName());
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final int WSSC_PREEXPIRE_SEC = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<Socket, Map<String, String>> socketCredentialCache =
            Collections.synchronizedMap(new WeakHashMap<Socket, Map<String, String>>());

    private final Ssg ssg;
    private final RequestInterceptor requestInterceptor;
    private final PolicyAttachmentKey policyAttachmentKey;
    private final URL originalUrl;
    private boolean policyUpdated = false;
    private boolean serverCertUpdated = false;
    private Long nonce = null; // nonce.  set on-demand, and only set once
    private String secureConversationId = null;
    private byte[] secureConversationSharedSecret = null;
    private Calendar secureConversationExpiryDate = null;
    private ClientSidePolicy clientSidePolicy = ClientSidePolicy.getPolicy();
    private SecretKey encryptedKeySecretKey = null;
    private String encryptedKeySha1 = null;
    private LoginCredentials requestCredentials = null;
    private boolean usedKerberosTicketReference = false;
    private String wsaNamespaceUri;
    private Socket clientSocket = null;
    private Map<String, String> clientSocketCredentials = null;

    // Policy settings, filled in by traversing policy tree, and which can all be rolled back by reset()
    private static class PolicySettings {
        private Policy activePolicy = null; // the policy that we most recently started applying to this request, if any
        private boolean isSslRequired = false;
        private boolean sslForbidden = false;  // ssl is forbidden for this request
        private boolean isBasicAuthRequired = false;
        private boolean isDigestAuthRequired = false;
        /**
         * these wss requirements are the default ones as opposed to the ones meant for a downstream recipient
         */
        private DecorationRequirements defaultWSSRequirements = null;
        private Map<String, DecorationRequirements> downstreamRecipientWSSRequirements = new HashMap<String, DecorationRequirements>();
        private String messageId = null;
        private Map<ClientAssertion, ClientDecorator> pendingDecorations = new LinkedHashMap<ClientAssertion, ClientDecorator>();
        public boolean useWsaMessageId;
        private DomainIdInjectionFlags domainIdInjectionFlags = new DomainIdInjectionFlags();
        private boolean ssgRequestedCompression = false;
    }

    private PolicySettings policySettings = new PolicySettings();

    public static class DomainIdInjectionFlags {
        public boolean enable = false;
        public String userHeaderName;
        public String domainHeaderName;
        public String processHeaderName;
    }

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
     * @throws IllegalStateException if this Message has not yet been attached to an InputStream.
     */
    public PolicyApplicationContext(Ssg ssg, Message request, Message response,
                                    RequestInterceptor requestInterceptor,
                                    PolicyAttachmentKey policyAttachmentKey,
                                    URL origUrl)
    {
        super(request, response);
        request.setEnableOriginalDocument();
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
     * Set the credentials that came in with the original request.  This may differ from the SSB's own credentials
     * as returned by getCredentials (or the CredentialManager), if chainCredentialsFromClient is enabled.
     * <p/>
     * This also defaults the current request credentials to the ones that arrived with the request.
     *
     * @param requestCredentials the credentials that arrived with the original request.
     */
    public void setRequestCredentials(LoginCredentials requestCredentials) {
        addCredentials(requestCredentials);
        this.requestCredentials = requestCredentials;
    }

    /**
     * Get the credentials that came in with the original request.  This may differ from the SSB's own credentials
     * as returned by getCredentials (or the CredentialManager), if chainCredentialsFromClient is enabled.
     *
     * @return the credentials that arrived with the original request, or null if there were none.
     */
    public LoginCredentials getRequestCredentials() {
        return requestCredentials;
    }

    /**
     * Reset all policy settings in preperation for starting processing over again with a different policy.
     *
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws SAXException if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream
     * @throws IOException if there is a problem reading from or writing to a stash
     * @throws UnsupportedOperationException if originalDocumentSupport is not enabled on this Message
     * @throws IllegalStateException if this Message has not yet been attached to an InputStream.
     */
    public void reset() throws SAXException, IOException {
        policySettings = new PolicySettings();
        final Document original = getRequest().getXmlKnob().getOriginalDocument();
        getRequest().getXmlKnob().setDocument((Document)original.cloneNode(true));
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

    /** @return true if a new policy has been downloaded for this request once already. */
    public boolean isPolicyUpdated() {
        return policyUpdated;
    }

    /** @return true if server cert discover has been performed for this request once already. */
    public boolean isServerCertUpdated() {
        return serverCertUpdated;
    }

    /** Note that server cert discovery has been performed for this SSG. */
    public void setServerCertUpdated() {
        serverCertUpdated = true;
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

    /**
     * Get the existing DecorationRequirements for this assertion's recipient, if this does not yet exist creates a new one
     * and set the recipient cert as part of the creation.
     *
     * @return the decoration requirements for the recipient designated by the passed recipient
     */
    public DecorationRequirements getWssRequirements(SecurityHeaderAddressable assertion) {
        return getAlternateWssRequirements(assertion.getRecipientContext());
    }

    /**
     * Get the existing DecorationRequirements for this recipient, if this does not yet exist creates a new one
     * and set the recipient cert as part of the creation.
     *
     * @return the decoration requirements for the recipient designated by the passed recipient
     */
    private DecorationRequirements getAlternateWssRequirements(XmlSecurityRecipientContext recipient) {
        if (recipient == null || recipient.localRecipient()) {
            return getDefaultWssRequirements();
        }
        String actor = recipient.getActor();
        DecorationRequirements output = policySettings.downstreamRecipientWSSRequirements.get(actor);
        if (output == null) {
            output = new DecorationRequirements();
            X509Certificate cert = recipient.getX509Certificate();
            output.setRecipientCertificate(cert);
            output.setSecurityHeaderActor(actor);
            policySettings.downstreamRecipientWSSRequirements.put(actor, output);
        }
        return output;
    }

    /**
     * Those requirements will be created the first time this is called.
     * Not intended to be used across multiple threads.
     * <p/>
     * Most assertions should NOT use this method.  Instead, they should use {@link #getWssRequirements(com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable)},
     * which will respect the SecurityHeaderAddressable setting.
     *
     * @return the decoration requirements for the immediate recipient (as opposed to further downstream recipients)
     */
    public DecorationRequirements getDefaultWssRequirements() {
        if (policySettings.defaultWSSRequirements == null) {
            policySettings.defaultWSSRequirements = new DecorationRequirements();
            if (ssg.isGeneric())
                policySettings.defaultWSSRequirements.setSecurityHeaderActor(null);

            final String overrideActor = ssg.getProperties().get(SsgRuntime.SSGPROP_DEFAULT_SECURITY_ACTOR);
            if (overrideActor != null)
                policySettings.defaultWSSRequirements.setSecurityHeaderActor(overrideActor);

            final String overrideMustUnderstand = ssg.getProperties().get(SsgRuntime.SSGPROP_SECURITY_MUSTUNDERSTAND);
            if (overrideMustUnderstand != null)
                policySettings.defaultWSSRequirements.setSecurityHeaderMustUnderstand("1".equals(overrideMustUnderstand) || Boolean.valueOf(overrideMustUnderstand));
        }
        return policySettings.defaultWSSRequirements;
    }

    /**
     * @return all decoration requirements (may return empty array)
     */
    public DecorationRequirements[] getAllDecorationRequirements() {
        Set<String> keys = policySettings.downstreamRecipientWSSRequirements.keySet();
        int size = 0;
        size += keys.size();
        if (policySettings.defaultWSSRequirements != null) {
            size++;
        }
        DecorationRequirements[] output = new DecorationRequirements[size];
        int i = 0;
        for (String key : keys) {
            output[i] = policySettings.downstreamRecipientWSSRequirements.get(key);
            i++;
        }
        if (policySettings.defaultWSSRequirements != null) {
            output[size-1] = policySettings.defaultWSSRequirements;
        }
        return output;
    }

    /**
     * @return the Map of (assertion instance => ClientDecorator), containing deferred decorations to apply.
     */
    public Map<ClientAssertion, ClientDecorator> getPendingDecorations() {
        return policySettings.pendingDecorations;
    }

    public String getMessageId() {
        return policySettings.messageId;
    }

    public void setUseWsaMessageId(boolean useWsaMessageId) {
        policySettings.useWsaMessageId = useWsaMessageId;
    }

    public boolean isUseWsaMessageId() {
        return policySettings.useWsaMessageId;
    }

    public void setMessageId(String newId) {
        policySettings.messageId = newId;
    }

    public String getUsername() throws OperationCanceledException, HttpChallengeRequiredException {
        PasswordAuthentication pa = getCredentialsForTrustedSsg();
        return pa==null ? null : pa.getUserName();
    }

    public char[] getPassword() throws OperationCanceledException, HttpChallengeRequiredException {
        PasswordAuthentication pa = getCredentialsForTrustedSsg();
        return pa==null ? null : pa.getPassword();
    }

    public long getNonce() {
        if (nonce == null)
            nonce = SECURE_RANDOM.nextLong();
        return nonce;
    }

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
            throws OperationCanceledException, HttpChallengeRequiredException {
        if (!ssg.isFederatedGateway())
            throw new OperationCanceledException("Trusted Gateway does not have any Federated credentials");
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted == null)
            // federated but no trustedGw, so must be using WS-Trust
            throw new OperationCanceledException("Federated Gateway that uses third-party WS-Trust does not have any Federated credentials");

        PasswordAuthentication pw = getPasswordAuthentication();
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1 || pw.getPassword() == null) {
            pw = ssg.getRuntime().getCredentialManager().getCredentials(trusted);
            if (pw != null)
                addCredentials(LoginCredentials.makePasswordCredentials(pw.getUserName(), pw.getPassword(), HttpBasic.class));
        }
        return pw;
    }

    /**
     * Get the login credentials.  Should not be used by Agent code.
     *
     * @deprecated Agent code should use getCredentialsForTrustedSsg() or getFederatedCredentials() instead.
     */
    public LoginCredentials getLastCredentials() {
        throw new UnsupportedOperationException();
    }

    private PasswordAuthentication getPasswordAuthentication() {
        LoginCredentials lc = super.getLastCredentials();
        if (lc == null) return null;
        return new PasswordAuthentication(lc.getLogin(), lc.getCredentials());
    }

    /**
     * Assert that credentials must be available to continue processing this request,
     * but that the existing ones were found to be no good.  Other than first throwing out
     * any existing configured credentials and displaying an error message,
     * this behaves like gatherCredentials().
     */
    public PasswordAuthentication getNewCredentials() throws OperationCanceledException, HttpChallengeRequiredException {
        PasswordAuthentication pw = ssg.getRuntime().getCredentialManager().getNewCredentials(ssg, true);
        if (pw != null)
            addCredentials(LoginCredentials.makePasswordCredentials(pw.getUserName(), pw.getPassword(), HttpBasic.class));
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
     * @throws UnsupportedOperationException if this is a federated SSG
     */
    public PasswordAuthentication getCredentialsForTrustedSsg() throws OperationCanceledException, HttpChallengeRequiredException {
        if (ssg.isFederatedGateway())
            throw new UnsupportedOperationException("Not permitted to send real password to Federated Gateway.");
        PasswordAuthentication pw = getPasswordAuthentication();
        if (pw == null || pw.getUserName() == null || pw.getUserName().length() < 1 || pw.getPassword() == null) {
            pw = ssg.getRuntime().getCredentialManager().getCredentials(ssg);
            if (pw != null)
                addCredentials(LoginCredentials.makePasswordCredentials(pw.getUserName(), pw.getPassword(), HttpBasic.class));
        }
        return pw;
    }

    /**
     * Get the currently-cached credentials for this request, or null if none are currently known.
     * This will NOT prompt the user for credentials, and will NOT (by itself) trigger an HTTP challenge.
     * It will simply return whatever credentials are already on hand.
     *
     * @return current credentials, or null if there aren't any.
     * @throws UnsupportedOperationException if this is a federated SSG
     */
    public PasswordAuthentication getCachedCredentialsForTrustedSsg() {
        if (ssg.isFederatedGateway())
            throw new UnsupportedOperationException("Not permitted to send real password to Federated Gateway.");
        return getPasswordAuthentication();
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
            BadCredentialsException, PolicyRetryableException, HttpChallengeRequiredException {
        try {
            if (ssg.getClientCertificate() == null) {
                logger.info("applying for client certificate");
                Ssg trusted = ssg.getTrustedGateway();
                if (trusted == null) {
                    if (ssg.isFederatedGateway())
                        throw new OperationCanceledException("Unable to apply for client cert from third-party WS-Trust server");
                    ssg.getRuntime().getSsgKeyStoreManager().obtainClientCertificate(getCredentialsForTrustedSsg());
                } else {
                    trusted.getRuntime().getSsgKeyStoreManager().obtainClientCertificate(getFederatedCredentials());
                    ssg.getRuntime().resetSslContext();
                }
            }
            else if(!ssg.isFederatedGateway()){ // the following is primarily for SAML SenderVouches
                logger.info("ensuring client certificate key is accessible");
                ssg.getClientCertificatePrivateKey(getCredentialsForTrustedSsg());
            }
        } catch (CertificateAlreadyIssuedException e) {
            // Bug #380 - if we haven't updated policy yet, try that first - mlyons
            //            but not if it's a federated gw, since in that case we may have needed the cert to DL the policy
            if (!isPolicyUpdated() && !ssg.isFederatedGateway()) {
                getSsg().getRuntime().getPolicyManager().flushPolicy(getPolicyAttachmentKey());
                throw new PolicyRetryableException();
            } else {
                Ssg certSsg = ssg.getTrustedGateway();
                if (certSsg == null) certSsg = ssg;
                ssg.getRuntime().getCredentialManager().notifyCertificateAlreadyIssued(certSsg);
                throw new OperationCanceledException("Unable to obtain a client certificate: " + ExceptionUtils.getMessage(e), e);
            }
        } catch (IOException e) {
            if (ExceptionUtils.causedBy(e, ServerCertificateUntrustedException.class))
                throw new ServerCertificateUntrustedException(e); // (this is a rethrow, so we preserve existing sslPeer)
            throw new ClientCertificateException("Unable to obtain a client certificate", e);
        } catch (ServerFeatureUnavailableException e) {
            Ssg certSsg = ssg.getTrustedGateway();
            if (certSsg == null) certSsg = ssg;
            ssg.getRuntime().getCredentialManager().notifyFeatureNotAvailable(certSsg, "certificate signing service");
            throw new OperationCanceledException("Unable to obtain a client certificate: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public String getWsaNamespaceUri() {
        return wsaNamespaceUri;
    }

    public void setWsaNamespaceUri(String wsaNamespaceUri) {
        this.wsaNamespaceUri = wsaNamespaceUri;
    }

    /**
     * Ensure that there is a Wsa message ID in this request.
     */
    public String prepareWsaMessageId(final boolean useWsa, String wsaNamespaceUri, final String messageIdPrefix) throws InvalidDocumentFormatException, SAXException, IOException {
        final String existingId = getMessageId();
        if (existingId != null) return existingId;

        String id = useWsa ?
                SoapUtil.getWsaMessageId(getRequest().getXmlKnob().getOriginalDocument(), wsaNamespaceUri) :
                SoapUtil.getL7aMessageId(getRequest().getXmlKnob().getOriginalDocument());

        if (id == null) {
            id = SoapUtil.generateUniqueUri(messageIdPrefix, useWsa);
        } else if (id.trim().length() < 1)
            throw new InvalidDocumentFormatException("Request has existing L7a:MessageID field that is empty or contains only whitespace");

        setUseWsaMessageId(useWsa);
        setWsaNamespaceUri(wsaNamespaceUri);
        setMessageId(id);
        return id;
    }

    private String establishSecureConversationSession()
            throws OperationCanceledException, GeneralSecurityException,
                   BadCredentialsException, IOException, ClientCertificateException, KeyStoreCorruptException, PolicyRetryableException, HttpChallengeRequiredException, ConfigurationException {
        Ssg ssg = getSsg();
        if (ssg.isGeneric())
            throw new ConfigurationException("No WS-SecureConversation token service is available for Gateway " + ssg);

        TokenServiceClient.SecureConversationSession s;

        WsTrustConfig wstConfig = WsTrustConfigFactory.getDefaultWsTrustConfig();
        TokenServiceClient tokenServiceClient = new TokenServiceClient(wstConfig, getHttpClient());

        // TODO support WS-SC with Http basic to fed SSG with third-party token service?
        if (ssg.isFederatedGateway())
            prepareClientCertificate();

        if (ssg.getClientCertificate() == null) {
            PasswordAuthentication pw = getCredentialsForTrustedSsg();
            logger.log(Level.INFO, "Establishing new WS-SecureConversation session with Gateway " + ssg.toString() + " using HTTP Basic over SSL");
            URL url = new URL("https", ssg.getSsgAddress(), ssg.getSslPort(), SecureSpanConstants.TOKEN_SERVICE_FILE);

            try {
                s = tokenServiceClient.obtainSecureConversationSessionWithSslAndOptionalHttpBasic(pw, url, ssg.getServerCertificateAlways());
            } catch (TokenServiceClient.UnrecognizedServerCertException e) {
                CurrentSslPeer.set(ssg);
                throw new ServerCertificateUntrustedException(e);
            } catch (TokenServiceClient.CertificateInvalidException cie) {
                throw new ClientCertificateRevokedException(cie.getMessage());
            }
        } else {
            logger.log(Level.INFO, "Establishing new WS-SecureConversation session with Gateway " + ssg.toString() + " using a WS-S signed request");
            URL url = new URL("http", ssg.getSsgAddress(), ssg.getSsgPort(), SecureSpanConstants.TOKEN_SERVICE_FILE);
            Date timestampCreatedDate = ssg.getRuntime().getDateTranslatorToSsg().translate(new Date());

            try {
                s = tokenServiceClient.obtainSecureConversationSessionUsingWssSignature(url, timestampCreatedDate,
                                                                                        ssg.getServerCertificateAlways(), ssg.getClientCertificate(),
                                                                                        ssg.getClientCertificatePrivateKey());
            } catch (TokenServiceClient.UnrecognizedServerCertException e) {
                CurrentSslPeer.set(ssg);
                throw new ServerCertificateUntrustedException(e);
            } catch (TokenServiceClient.CertificateInvalidException cie) {
                throw new ClientCertificateRevokedException(cie.getMessage());
            }
        }
        logger.log(Level.INFO, "WS-SecureConversation session established with Gateway " + ssg.toString() + "; session ID=" + s.getSessionId());
        ssg.getRuntime().secureConversationId(s.getSessionId());
        secureConversationId = s.getSessionId();
        ssg.getRuntime().secureConversationSharedSecret(s.getSharedSecret());
        secureConversationSharedSecret = s.getSharedSecret();
        if (s.getExpiryDate() == null) {
            logger.info("WS-SecureConversation session did not include an expiry date.  Assuming expiry 600 seconds from now.");
            Calendar expiry = Calendar.getInstance(UTC_TIME_ZONE);
            expiry.add(Calendar.SECOND, WSSC_PREEXPIRE_SEC);
            secureConversationExpiryDate = expiry;
            ssg.getRuntime().secureConversationExpiryDate(expiry);
        } else {
            Calendar expiry = Calendar.getInstance(UTC_TIME_ZONE);
            expiry.setTime(ssg.getRuntime().getDateTranslatorFromSsg().translate(s.getExpiryDate()));
            secureConversationExpiryDate = expiry;
            ssg.getRuntime().secureConversationExpiryDate(expiry);
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
                    Calendar ssgDate = ssg.getRuntime().secureConversationExpiryDate();
                    if (ssgDate == secureConversationExpiryDate || (ssgDate != null && !ssgDate.after(now))) {
                        logger.log(Level.INFO, "Our WS-SecureConversation session has expired or will do so within the next " +
                          WSSC_PREEXPIRE_SEC + " seconds.  Will throw it away and get a new one.");
                        ssg.getRuntime().secureConversationId(null);
                        ssg.getRuntime().secureConversationSharedSecret(null);
                        ssg.getRuntime().secureConversationExpiryDate(null);
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
            ClientCertificateException, BadCredentialsException, PolicyRetryableException, ConfigurationException,
            HttpChallengeRequiredException {
        checkExpiredSecureConversationSession();
        if (secureConversationId != null)
            return secureConversationId;

        secureConversationId = ssg.getRuntime().secureConversationId();
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
            ssg.getRuntime().secureConversationId(null);
            ssg.getRuntime().secureConversationExpiryDate(null);
            ssg.getRuntime().secureConversationSharedSecret(null);
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
        return secureConversationSharedSecret = ssg.getRuntime().secureConversationSharedSecret();
    }

    /**
     * Get a valid holder-of-key SAML assertion for this SSG (or the Trusted SSG if this is a federated SSG).
     * If we don't currently hold a valid holder-of-key SAML assertion we will apply for a new one.
     *
     * @param version The SAML version (0 for any version)
     * @return A SAML assertion with us as the subject and our trusted SSG as the issuer.  Never null.
     * @throws OperationCanceledException if the user cancels the login dialog
     * @throws GeneralSecurityException   if there is a problem with a certificate, key, or signature
     * @throws IOException                if there is a problem reading from the network or a file
     * @throws KeyStoreCorruptException   if our local key store or trust store is damaged
     * @throws ClientCertificateException if we need a client certificate but can't obtain one
     * @throws BadCredentialsException    if we need a certificate but our username and password is wrong
     * @throws PolicyRetryableException   if we should retry policy processing from the beginning
     * @throws IllegalArgumentException   if an unsupported SAML version is requested
     * @throws ConfigurationException     if there's no SAML token strategy for this SSG.  Shouldn't happen.
     */
    public SamlAssertion getOrCreateSamlHolderOfKeyAssertion(int version)
            throws OperationCanceledException, GeneralSecurityException, IOException, KeyStoreCorruptException,
            ClientCertificateException, BadCredentialsException, PolicyRetryableException, ConfigurationException, HttpChallengeRequiredException {
        TokenStrategy samlStrat;

        if (version == 1) {
            samlStrat = ssg.getRuntime().getTokenStrategy(SecurityTokenType.SAML_ASSERTION);
        }
        else if (version == 2 || version == 0) {
            samlStrat = ssg.getRuntime().getTokenStrategy(SecurityTokenType.SAML2_ASSERTION);
        }
        else {
            throw new IllegalArgumentException("Unsupported SAML version '" + version + "'.");
        }

        if (samlStrat == null)
            throw new ConfigurationException("No SAML token strategy is available for SSG " + ssg);

        return (SamlAssertion)samlStrat.getOrCreate(ssg);
    }

    /**
     * Get a valid sender-vouches assertion for this SSG, vouching for the current RequestCredentials username.
     *
     * @return a sender-vouches SAML assertion.  Never null.
     */
    public SamlAssertion getOrCreateSamlSenderVouchesAssertion(int version)
            throws ConfigurationException, OperationCanceledException, GeneralSecurityException,
            IOException, KeyStoreCorruptException, ClientCertificateException, BadCredentialsException,
            PolicyRetryableException, HttpChallengeRequiredException
    {

        Object username = null;

        // Try a separate username from the client first
        SecurityKnob sk = (SecurityKnob)getRequest().getKnob(SecurityKnob.class);
        if (sk != null) {
            List<SecurityToken> tokens = sk.getAllSecurityTokens();
            if (tokens != null) for (SecurityToken token : tokens) {
                if (token instanceof HasUsername) {
                    username = ((HasUsername)token).getUsername();
                }
            }
        }

        if (username == null) {
            LoginCredentials creds = getRequestCredentials();
            if (creds != null) {
                username = creds.getLogin();
            } else {
                if (ssg.isChainCredentialsFromClient())
                    throw new HttpChallengeRequiredException("username and password required");

                PasswordAuthentication pw = ssg.getRuntime().getCredentials();
                if (pw == null) {
                    // If it's a WS-Trust STS Gateway registration, try anyway, with a global cache key
                    if (ssg.isFederatedGateway() && ssg.getTrustedGateway() == null) {
                        logger.info("Unable to find a user to vouch for with sender-vouches.  Falling back to generic token strategy");
                        username =  WsTrustSamlTokenStrategy.class;
                    } else
                        throw new ConfigurationException("Unable to create sender vouches assertion -- no username to vouch for");
                } else
                    username = pw.getUserName();
            }
        }

        Map<String, String> props = ssg.getProperties();
        final String nameIdFormatUri;
        final String authnMethodUri;
        final String nameIdTemplate;
        final TokenStrategy strategy;
        if (version == 1) {
            nameIdFormatUri = props.get("builtinSaml1.nameIdFormatUri");
            authnMethodUri = props.get("builtinSaml1.authenticationMethodUri");
            nameIdTemplate = props.get("builtinSaml1.nameIdTemplate");
            strategy = ssg.getRuntime().getSenderVouchesStrategyForUsername(SecurityTokenType.SAML_ASSERTION, username, nameIdFormatUri, authnMethodUri, nameIdTemplate);
        } else if (version == 2 || version == 0) {
            nameIdFormatUri = props.get("builtinSaml2.nameIdFormatUri");
            authnMethodUri = props.get("builtinSaml2.authenticationMethodUri");
            nameIdTemplate = props.get("builtinSaml2.nameIdTemplate");
            strategy = ssg.getRuntime().getSenderVouchesStrategyForUsername(SecurityTokenType.SAML2_ASSERTION, username, nameIdFormatUri, authnMethodUri, nameIdTemplate);
        } else {
            throw new IllegalArgumentException("Unsupported SAML version '" + version + "'.");
        }
        return (SamlAssertion) strategy.getOrCreate(ssg);
    }

    /**
     * Clear the Kerberos ticket.
     */
    public void clearKerberosServiceTicket() {
        setUsedKerberosServiceTicketReference(false);
        ssg.getRuntime().kerberosTicket(null);
    }

    /**
     * Get the id of a previously issued GSS AP-REQ Ticket.
     *
     * @return the id or null.
     */
    public String getKerberosServiceTicketId() {
        String id = null;

        KerberosServiceTicket kst = ssg.getRuntime().kerberosTicket();
        if(kst!=null) {
            Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
            now.add(Calendar.SECOND, WSSC_PREEXPIRE_SEC);

            if(kst.getExpiry() <= now.getTime().getTime()) {
                logger.log(Level.INFO, "Our Kerberos session has expired or will do so within the next " +
                          WSSC_PREEXPIRE_SEC + " seconds. Will throw it away and get a new one.");
            }
            else {
                id = HexUtils.encodeBase64(HexUtils.getSha1Digest(kst.getGSSAPReqTicket().toByteArray()));
            }
        }

        return id;
    }

    /**
     * Check if a reference to a previously used Kerberos token was used in the request.
     *
     * @return true if a reference was used.
     */
    public boolean usedKerberosServiceTicketReference() {
        return usedKerberosTicketReference;
    }

    /**
     * Set the flag for a Kerberos token.
     *
     * @param used true if a reference was used.
     */
    public void setUsedKerberosServiceTicketReference(boolean used) {
        usedKerberosTicketReference = used;
    }

    /**
     * Get the Kerberos ticket for the existing session (if any)
     *
     * <p>Before calling this method call getKerberosServiceTicketId(), if is it null then
     * get a new ticket (dont call this method).</p>
     *
     * @return the ticket or null.
     */
    public KerberosServiceTicket getExistingKerberosServiceTicket() {
        return ssg.getRuntime().kerberosTicket();
    }

    /**
     * Get a valid Kerberos GSS AP-REQ Ticket.
     *
     * @return the ticket
     * @throws GeneralSecurityException is a ticket could not be obtained.
     */
    public KerberosServiceTicket getKerberosServiceTicket() throws
            GeneralSecurityException, OperationCanceledException, HttpChallengeRequiredException
    {
        final int flagCanceled = 0;
        final int flagChallenge = 1;
        final boolean[] flags = new boolean[2];
        try {
            // determine the kerberos service/host
            String kerberosName = ssg.getKerberosName();
            if(kerberosName==null || kerberosName.trim().length()==0) kerberosName = ssg.getHostname();
            String serviceName = null;
            int split = kerberosName.indexOf('/');
            final String hostName = kerberosName.substring(split+1);
            if(split>0) serviceName = kerberosName.substring(0,split);

            // get ticket
            KerberosClient client = new KerberosClient();
            client.setCallbackHandler(new CallbackHandler(){
                public void handle(Callback[] callbacks) {
                    if(!flags[flagCanceled]) {
                        try {
                            PasswordAuthentication pa = getCredentialsForTrustedSsg();
                            for (Callback callback : callbacks) {
                                if (callback instanceof PasswordCallback) {
                                    PasswordCallback pc = (PasswordCallback) callback;
                                    if (pa != null) pc.setPassword(pa.getPassword());
                                    else pc.setPassword(new char[]{' '});
                                } else if (callback instanceof NameCallback) {
                                    NameCallback nc = (NameCallback) callback;
                                    if (pa != null) nc.setName(pa.getUserName());
                                    else nc.setName(System.getProperty("user.name", ""));
                                }
                            }
                        }
                        catch(OperationCanceledException oce) {
                            flags[flagCanceled] = true;
                        } catch (HttpChallengeRequiredException e) {
                            flags[flagChallenge] = true;
                        }
                    }
                }
            });

            if (flags[flagChallenge])
                throw new HttpChallengeRequiredException();

            KerberosServiceTicket kst = client.getKerberosServiceTicket(KerberosClient.getServicePrincipalName(serviceName,hostName), false);

            setUsedKerberosServiceTicketReference(false);
            ssg.getRuntime().kerberosTicket(kst);

            return kst;
        }
        catch(KerberosException ke) {
            if(flags[flagCanceled])
                throw new OperationCanceledException("User did not supply credentials.");
            if (flags[flagChallenge])
                throw new HttpChallengeRequiredException();
            if(ExceptionUtils.causedBy(ke, LoginException.class)) {
                getNewCredentials();
                return getKerberosServiceTicket();
            }
            else {
                throw new GeneralSecurityException("Could not get Kerberos Ticket", ke);
            }
        }
    }

    /**
     * Give this PAC access to the low level client socket.
     * This is required to use {@link #getClientSocketCredentials}.
     *
     * @param clientSocket the client socket.
     */
    public void setClientSocket(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Get the client socket credentials, if available.
     * This requires that the raw client socket has been registered with this PAC using {@link #setClientSocket}.
     *
     * @return a Map that contains client socket credentials, or null if none are available.
     *         <p/>
     *         The Map keys are identifiers documented for {@link LocalTcpPeerIdentifier}.
     */
    public Map<String, String> getClientSocketCredentials() {
        if (clientSocketCredentials != null)
            return clientSocketCredentials;

        Socket sock = this.clientSocket;
        if (sock == null)
            return null;

        Map<String, String> ret = socketCredentialCache.get(sock);
        if (ret != null)
            return ret;

        if (!LocalTcpPeerIdentifierFactory.isAvailable())
            return null;

        LocalTcpPeerIdentifier ident = LocalTcpPeerIdentifierFactory.createIdentifier();

        try {
            ident.identifyTcpPeer(sock);
            this.clientSocketCredentials = ret = ident.getIdentifierMap();
            socketCredentialCache.put(sock, ret);
            return ret;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to gather local client socket credentials: " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    /**
     * Download a new policy for this request.
     * @param serviceid the service ID reported in the Policy-Url: header.
     * @throws IOException if the policy could not be read from the SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException if an SSL handshake with the SSG could not be established due to
     *                                             the SSG's SSL certificate being unrecognized
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if credentials were required, but the user canceled the logon dialog
     */
    public void downloadPolicy(String serviceid)
            throws OperationCanceledException, GeneralSecurityException, HttpChallengeRequiredException,
            IOException, ClientCertificateException, KeyStoreCorruptException, PolicyRetryableException,
            ConfigurationException, PolicyLockedException
    {
        final Ssg ssg = getSsg();
        if (!ssg.isPolicyDiscoverySupported())
            throw new OperationCanceledException("Unable to download policy: Gateway " + ssg + " does not support automatic policy discovery");
        final PolicyAttachmentKey pak = new PolicyAttachmentKey(getPolicyAttachmentKey());
        // TODO replace this mess of exceptions with an exception base interface that sports a handle() method
        try {
            final Policy policy = new PolicyDownloader(this).downloadPolicy(pak, serviceid);
            ssg.getRuntime().getPolicyManager().setPolicy(pak, policy);
            if (requestInterceptor != null) requestInterceptor.onPolicyUpdated(ssg, pak, policy);
            policyUpdated = true;
            ssg.getRuntime().clearSessionCookies();
            logger.info("New policy saved successfully");
        } catch (ServerCertificateUntrustedException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new ServerCertificateUntrustedException(e);
        } catch (ConfigurationException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new ConfigurationException(e);
        } catch (OperationCanceledException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new OperationCanceledException(e);
        } catch (GeneralSecurityException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new GeneralSecurityException(e);
        } catch (HttpChallengeRequiredException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new HttpChallengeRequiredException(e);
        } catch (IOException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new CausedIOException(e);
        } catch (ClientCertificateRevokedException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw e;
        } catch (ClientCertificateException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new ClientCertificateException(e);
        } catch (KeyStoreCorruptException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new KeyStoreCorruptException(e);
        } catch (PolicyRetryableException e) {
            if (requestInterceptor != null) requestInterceptor.onPolicyError(ssg, pak, e);
            logger.warning("Policy download failed: " + ExceptionUtils.getMessage(e));
            throw new PolicyRetryableException(e);
        }
    }

    /**
     * Call through to runtime to get SimpleHttpClient (allows BRA to override)
     */
    public SimpleHttpClient getHttpClient() {
        return getSsg().getRuntime().getHttpClient();
    }

    /**
     *  Call through to runtime to get cookies (allows BRA to override)
     */
    public HttpCookie[] getSessionCookies() {
        return getSsg().getRuntime().getSessionCookies();
    }

    /**
     * Call through to runtime to set cookies (allows BRA to override)
     */
    public void setSessionCookies(HttpCookie[] cookies) {
        getSsg().getRuntime().setSessionCookies(cookies);
    }

    /**
     * Get the SecretKey that was encoded into an EncryptedKey in the request message, if we encoded one.
     * Use @{link #getEncryptedKeySha1} to save the EncryptedKeySHA1 reference that can be used to recognize
     * future references to this SecretKey.
     *
     * @return the SecretKey we encoded into the request message EncryptedKey, or null if we didn't encode one.
     */
    public SecretKey getEncryptedKeySecretKey() {
        return encryptedKeySecretKey;
    }

    /**
     * Save the SecretKey that was encoded into an EncryptedKey in the request message.  Typically called
     * only by {@link com.l7tech.proxy.processor.MessageProcessor}, after it decorates the request.
     *
     * @param encryptedKeySecretKey the secret key included in the request, or null if there wasn't one.
     */
    public void setEncryptedKeySecretKey(SecretKey encryptedKeySecretKey) {
        this.encryptedKeySecretKey = encryptedKeySecretKey;
    }

    /**
     * Get the EncryptedKeySHA1 identifier for any EncryptedKey encoded into the request message.
     *
     * @return the identifier for the EncryptedKey we encoded into the request message, or null if we didn't
     *         encode an EncryptedKey into the request message.
     */
    public String getEncryptedKeySha1() {
        return encryptedKeySha1;
    }

    /**
     * Save the EncryptedKeySHA1 identifier for any EncryptedKey encoded into the request message.
     * Typically called only by {@link com.l7tech.proxy.processor.MessageProcessor}, after it decorates the
     * request.
     *
     * @param encryptedKeySha1 the identifier for the EncryptedKey we encoded into the request, or null if there wasn't one.
     */
    public void setEncryptedKeySha1(String encryptedKeySha1) {
        this.encryptedKeySha1 = encryptedKeySha1;
    }

    public DomainIdInjectionFlags getDomainIdInjectionFlags() {
        return policySettings.domainIdInjectionFlags;
    }

    public boolean isSsgRequestedCompression() {
        return policySettings.ssgRequestedCompression;
    }

    public void setSsgRequestedCompression(boolean ssgRequestedCompression) {
        this.policySettings.ssgRequestedCompression = ssgRequestedCompression;
    }
}
