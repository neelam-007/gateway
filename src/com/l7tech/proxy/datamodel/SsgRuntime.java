/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.DateTranslator;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.ssl.ClientProxyKeyManager;
import com.l7tech.proxy.ssl.ClientProxyTrustManager;
import com.l7tech.proxy.util.TokenServiceClient;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds SSG behaviour, strategies, and transient state.
 */
public class SsgRuntime {
    private static final Logger log = Logger.getLogger(SsgRuntime.class.getName());

    private final Ssg ssg;

    // SAML timestamp stuff
    private static final int SAML_PREEXPIRE_SEC = 30;
    // Maximum simultaneous outbound connections.  Throttled wide-open.
    public static final int MAX_CONNECTIONS = 60000;

    private PolicyManager rootPolicyManager = null; // policy store that is not saved to disk
    private char[] password = null;
    private boolean promptForUsernameAndPassword = true;
    private KeyStore keyStore = null;
    private KeyStore trustStore = null;
    private Boolean haveClientCert = null;
    private int numTimesLogonDialogCanceled = 0;
    private long credentialsUpdatedTimeMillis = 0;
    private PrivateKey privateKey = null; // cache of private key
    private boolean passwordWorkedForPrivateKey = false;
    private boolean passwordWorkedWithSsg = false;
    private SSLContext sslContext = null;
    private ClientProxyTrustManager trustManager = null;
    private Cookie[] sessionCookies = null;
    private X509Certificate serverCert = null;
    private X509Certificate clientCert = null;
    private byte[] secureConversationSharedSecret = null;
    private String secureConversationId = null;
    private Calendar secureConversationExpiryDate = null;
    private long timeOffset = 0;
    private Map tokenStrategiesByType;
    private final MultiThreadedHttpConnectionManager httpConnectionManager = new MultiThreadedHttpConnectionManager();

    private DateTranslator fromSsgDateTranslator = new DateTranslator() {
        public Date translate(Date source) {
            if (source == null)
                return null;
            final long timeOffset = getTimeOffset();
            if (timeOffset == 0)
                return source;
            final Date result = new Date(source.getTime() - timeOffset);
            log.log(Level.FINE, "Translating date from SSG clock " + source + " to local clock " + result);
            return result;
        }
    };

    private DateTranslator toSsgDateTranslator = new DateTranslator() {
        public Date translate(Date source) {
            if (source == null)
                return null;
            final long timeOffset = getTimeOffset();
            if (timeOffset == 0)
                return source;
            final Date result = new Date(source.getTime() + timeOffset);
            log.log(Level.FINE, "Translating date from local clock " + source + " to SSG clock " + result);
            return result;
        }
    };

    public SsgRuntime(Ssg ssg) {
        if (ssg == null) throw new NullPointerException();
        this.ssg = ssg;
        httpConnectionManager.setMaxConnectionsPerHost(MAX_CONNECTIONS);
        httpConnectionManager.setMaxTotalConnections(MAX_CONNECTIONS);
    }

    /** Flush any cached data from the key store. */
    void flushKeyStoreData() {
        synchronized (ssg) {
            keyStore = null;
            trustStore = null;
            haveClientCert = null;
            privateKey = null;
            passwordWorkedForPrivateKey = false;
        }
    }

    /**
     * @return the root PolicyManager for this SSG.  Never null.
     */
    public PolicyManager rootPolicyManager() {
        if (rootPolicyManager == null) {
            synchronized (ssg) {
                if (rootPolicyManager == null) {
                    rootPolicyManager = new TransientPolicyManager(ssg.getPersistentPolicyManager());
                }
            }
        }
        return rootPolicyManager;
    }

    /** Replace the root policy manager.  This should never be called by a production class; it is here only for test purposes. */
    public void rootPolicyManager(PolicyManager p) {
        rootPolicyManager = p;
    }

    /**
     * Read the in-memory cached password for this SSG. Should be used only by a CredentialManager
     * or the SsgPropertiesDialog.
     * Others should use the CredentialManager's getCredentialsForTrustedSsg() method instead.
     *
     * @return the password, or null if we don't have one in memory for this SSG
     */
    public char[] getCachedPassword() {
        synchronized (ssg) {
            if (password == null)
                password = Ssg.deobfuscatePassword(ssg.getPersistPassword());
            return password;
        }
    }

    /**
     * Set the in-memory cached password for this SSG.  Should be used only by a CredentialManager
     * or the SsgPropertyDialog.
     * Others should go through the PolicyApplicationContext, if there is one, or else go through the CredentialManager.
     *
     * @param password
     */
    public void setCachedPassword(final char[] password) {
        synchronized (ssg) {
            if (this.password != password) {
                this.passwordWorkedForPrivateKey = false;
                this.passwordWorkedWithSsg = false;
            }
            this.password = password;

            // clear session cookies when a user name is changed/set
            clearSessionCookies();

            if (ssg.isSavePasswordToDisk())
                ssg.setPersistPassword(Ssg.obfuscatePassword(password));
            else
                ssg.setPersistPassword(null);
        }
    }

    /** Check if the currently-configured password is known to have worked with the SSG. */
    public boolean passwordWorkedWithSsg() {
        return passwordWorkedWithSsg;
    }

    public void passwordWorkedWithSsg(boolean worked) {
        passwordWorkedWithSsg = worked;
    }

    /**
     * Transient record of shared secret for WS-SecureConversation session.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public byte[] secureConversationSharedSecret() {
        return secureConversationSharedSecret;
    }

    /**
     * Transient record of shared secret for WS-SecureConversation session.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public void secureConversationSharedSecret(byte[] secureConversationSharedSecret) {
        this.secureConversationSharedSecret = secureConversationSharedSecret;
    }

    /**
     * Transient record of session unique ID URI for WS-SecureConversation session.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public String secureConversationId() {
        return secureConversationId;
    }

    /**
     * Transient record of session unique ID URI for WS-SecureConversation session.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public void secureConversationId(String secureConversationId) {
        this.secureConversationId = secureConversationId;
    }

    /**
     * Transient record of session expiry date for WS-SecureConversation session.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public Calendar secureConversationExpiryDate() {
        return secureConversationExpiryDate;
    }

    /**
     * Transient record of session expiry date for WS-SecureConversation session.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public void secureConversationExpiryDate(Calendar secureConversationExpiryDate) {
        this.secureConversationExpiryDate = secureConversationExpiryDate;
    }

    public boolean promptForUsernameAndPassword() {
        return promptForUsernameAndPassword;
    }

    public void promptForUsernameAndPassword(boolean promptForUsernameAndPassword) {
        this.promptForUsernameAndPassword = promptForUsernameAndPassword;
    }

    /** Transient in-core cache of KeyStore.  Package private; used by SsgKeyStoreManager. */
    KeyStore keyStore() {
        return keyStore;
    }

    /** Transient in-core cache of KeyStore.  Package private; used by SsgKeyStoreManager. */
    void keyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /** Transient in-core cache of TrustStore.  Package private; used by SsgKeyStoreManager. */
    KeyStore trustStore() {
        return trustStore;
    }

    /** Transient in-core cache of TrustStore.  Package private; used by SsgKeyStoreManager. */
    void trustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
    }

    /**
     * Transient quick check of whether we have a client cert or not.
     * Package-private; used by SsgKeyStoreManager.
     * @return true if we have a client cert; false if we don't; null if we haven't looked yet
     */
    Boolean getHaveClientCert() {
        return haveClientCert;
    }

    /**
     * Transient quick check of whether we have a client cert or not.
     * Package-private; used by SsgKeyStoreManager.
     * @param haveClientCert true = we have one; false = we don't; null = we haven't looked yet
     */
    void setHaveClientCert(Boolean haveClientCert) {
        this.haveClientCert = haveClientCert;
    }

    /** Transient cache of private key for client cert in keystore; used by SsgKeyStoreManager. */
    PrivateKey getCachedPrivateKey() {
        return privateKey;
    }

    /** Transient cache of private key for client cert in keystore; used by SsgKeyStoreManager. */
    void setCachedPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /** Transient check if this password worked to unlock the private key; used by SsgKeyStoreManager. */
    void setPasswordCorrectForPrivateKey(boolean worked) {
        this.passwordWorkedForPrivateKey = worked;
    }

    /** Transient check if this password worked to unlock the private key; used by SsgKeyStoreManager. */
    boolean isPasswordCorrectForPrivateKey() {
        return this.passwordWorkedForPrivateKey;
    }

    public int incrementNumTimesLogonDialogCanceled() {
        synchronized (ssg) {
            return numTimesLogonDialogCanceled++;
        }
    }

    public void onCredentialsUpdated() {
        credentialsUpdatedTimeMillis = System.currentTimeMillis();
    }

    public long getCredentialsLastUpdatedTime() {
        return credentialsUpdatedTimeMillis;
    }

    /**
     * Get the configured credentials for this Ssg, if any.
     * @return The configuration credentials, or null if there aren't any yet.
     */
    public PasswordAuthentication getCredentials() {
        String username = ssg.getUsername();
        char[] password = getCachedPassword();
        if (username != null && ssg.getTrustedGateway() != null)
            return new PasswordAuthentication(username, new char[0]); // shield actual password in federated case
        if (username != null && username.length() > 0 && password != null)
            return new PasswordAuthentication(username, password);
        return null;
    }

    /**
     * Get the strategy for obtaining a specific type of security token.
     * <p>
     * TODO make these strategies configurable somehow
     *
     * @param tokenType the type of security token to obtain
     * @return the strategy for getting the requested security token for this Ssg, or null if this Ssg
     *          is not configured with a strategy for the requested token type.
     */
    public TokenStrategy getTokenStrategy(SecurityTokenType tokenType) {
        synchronized (ssg) {
            if (tokenStrategiesByType == null) {
                tokenStrategiesByType = new HashMap();
                Ssg tokenServerSsg = ssg.getTrustedGateway();
                if (tokenServerSsg == null) tokenServerSsg = ssg;
                tokenStrategiesByType.put(SecurityTokenType.SAML_AUTHENTICATION,
                                          new DefaultSamlAuthnTokenStrategy(tokenServerSsg));
            }
            return (TokenStrategy)tokenStrategiesByType.get(tokenType);
        }
    }

    /** Get the trust manager used for SSL connections to this SSG. */
    public ClientProxyTrustManager getTrustManager() {
        synchronized (ssg) {
            if (trustManager == null) {
                trustManager = SslInstanceHolder.trustManager;
            }
            return trustManager;
        }
    }

    /** Set the trust manager to use for SSL connections to this SSG. */
    public void setTrustManager(ClientProxyTrustManager tm) {
        synchronized (ssg) {
            if (tm == null)
                throw new IllegalArgumentException("TrustManager may not be null");
            trustManager = tm;
        }
    }

    /**
     * Establish or reestablish the global SSL state.  Must be called after any change to client
     * or server certificates used during any SSL handshake, otherwise the implementation may cache
     * undesirable information.  (The cache is seperate from the session cache, too, so you can't just
     * flush the sessions to fix it.)
     */
    private SSLContext createSslContext()
    {
        synchronized (ssg) {
            log.info("Creating new SSL context for SSG " + toString());
            ClientProxyKeyManager keyManager = SslInstanceHolder.keyManager;
            ClientProxyTrustManager trustManager = getTrustManager();
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSL", System.getProperty("com.l7tech.proxy.sslProvider",
                                                                              "SunJSSE"));
                sslContext.init(new X509KeyManager[] {keyManager},
                                new X509TrustManager[] {trustManager},
                                null);
                return sslContext;
            } catch (NoSuchAlgorithmException e) {
                log.log(Level.SEVERE, "Unable to create SSL context", e);
                throw new RuntimeException(e); // shouldn't happen
            } catch (NoSuchProviderException e) {
                log.log(Level.SEVERE, "Unable to create SSL context", e);
                throw new RuntimeException(e); // shoudn't happen
            } catch (KeyManagementException e) {
                log.log(Level.SEVERE, "Unable to create SSL context", e);
                throw new RuntimeException(e); // shouldn't happen
            }
        }
    }

    public SSLContext getSslContext() {
        synchronized (ssg) {
            if (sslContext == null)
                sslContext = createSslContext();
            return sslContext;
        }
    }

    /** Flush SSL context and cached certificates. */
    public void resetSslContext()
    {
        synchronized (ssg) {
            keyStore(null);
            trustStore(null);
            setCachedPrivateKey(null);
            setPasswordCorrectForPrivateKey(false);
            setHaveClientCert(null);
            setCachedClientCert(null);
            setCachedServerCert(null);
            clearSessionCookies();
            sslContext = createSslContext();
            serverCert = null;
            clientCert = null;
        }
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public X509Certificate getCachedServerCert() {
        synchronized (ssg) {
            return serverCert;
        }
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public void setCachedServerCert(X509Certificate cert) {
        synchronized (ssg) {
            this.serverCert = cert;
        }
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public X509Certificate getCachedClientCert() {
        synchronized (ssg) {
            return clientCert;
        }
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public void setCachedClientCert(X509Certificate cert) {
        synchronized (ssg) {
            this.clientCert = cert;
        }
    }

    /**
     * Return the HTTP cookies of the user session established with SiteMinder Policy Server.
     *
     * @return  Cookie[]  The list of session cookies.
     */
    public Cookie[] getSessionCookies() {
        synchronized (ssg) {
            String cookieString = "HTTP cookies: ";
            if (sessionCookies != null && sessionCookies.length > 0) {

                for (int i = 0; i < sessionCookies.length; i++) {
                    cookieString += "[ " + sessionCookies[i].toExternalForm() + " ], ";
                }
            } else {
                cookieString += " empty";
            }

            log.info(cookieString);
            return sessionCookies;
        }
    }

    /**
     * Store the HTTP cookies of the user session established with SiteMinder Policy Server.
     *
     * @param cookies  The HTTP cookies to be saved.
     */
    public void setSessionCookies(Cookie[] cookies) {
        synchronized (ssg) {
            sessionCookies = cookies;
        }
    }

    /**
     * Delete the cookies.
     */
    public void clearSessionCookies() {
        synchronized (ssg) {
            sessionCookies = new Cookie[0];
        }
    }

    /**
     * Get the time offset for this SSG.  If set, this is the approximate number of milliseconds
     * that must be added to the Bridge's local UTC time to match the UTC time set on this SSG.  This
     * value might be negative if the Bridge's clock is ahead of the SSG's.
     * <p>
     * This is used for clock-skew workaround to enable the Bridge to interoperate with multiple SSGs
     * from different organizations, each of which might have different clocks.  As long as the Bridge
     * does not need to pass timestamps between two different SSGs with differing opinions about the
     * current time (as when getting a SAML token from one and presenting it to the other, for example)
     * this mechanism enables the Bridge to work around the problem.
     *
     * @return the time offset, if set; otherwise 0.
     */
    public long getTimeOffset() {
        return timeOffset;
    }

    /**
     * Set a time offset for this SSG.  See getTimeOffset() for details.
     * @param timeOffset the new offset to use, or 0 to disable clock translation.
     */
    public void setTimeOffset(long timeOffset) {
        this.timeOffset = timeOffset;
    }

    /**
     * Translate a date and time from the SSG's clock into our local clock.  Leaves the
     * date unchanged if a TimeOffset is not set for this SSG.  This is used to work around
     * clock-skew between the Bridge and this SSG; see getTimeOffset() for details.
     * @return a DateTranslator that will translate according to the Bridge's local clock setting
     */
    public DateTranslator getDateTranslatorFromSsg() {
        return fromSsgDateTranslator;
    }

    /**
     * Translate a date and time from the Bridge's local clock into the SSG's clock.  Leaves
     * the date unchanged if a TimeOffset is not set for this SSG.  This is used to work around
     * clock-skew between the Bridge and this SSG; see getTimeOFfset() for details.
     * @return a DateTranslator that will translate according to the SSG's clock setting
     */
    public DateTranslator getDateTranslatorToSsg() {
        return toSsgDateTranslator;
    }

    public MultiThreadedHttpConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }

    // Hack to allow lazy initialization of SSL stuff
    private static class SslInstanceHolder {
        private static final ClientProxyKeyManager keyManager = new ClientProxyKeyManager();
        private static final ClientProxyTrustManager trustManager = new ClientProxyTrustManager();
    }

    private static class DefaultSamlAuthnTokenStrategy extends AbstractTokenStrategy {
        private final Ssg tokenServerSsg;
        private SamlAssertion cachedAssertion = null;

        /**
         * @param tokenServerSsg what SSG is going to give me a SAML token
         */
        public DefaultSamlAuthnTokenStrategy(Ssg tokenServerSsg)
        {
            super(SecurityTokenType.SAML_AUTHENTICATION);
            if (tokenServerSsg == null) throw new NullPointerException();
            this.tokenServerSsg = tokenServerSsg;
        }

        public SecurityToken getOrCreate()
                throws OperationCanceledException, GeneralSecurityException, IOException, ClientCertificateException,
                KeyStoreCorruptException, PolicyRetryableException, BadCredentialsException
        {
            synchronized (tokenServerSsg) {
                removeIfExpired();
                if (cachedAssertion != null)
                    return cachedAssertion;

            }
            SamlAssertion newone = acquireSamlAssertion();
            synchronized (tokenServerSsg) {
                return cachedAssertion = newone;
            }
        }

        public SecurityToken getIfPresent() {
            synchronized (tokenServerSsg) {
                removeIfExpired();
                return cachedAssertion;
            }
        }

        /**
         * Flush cached assertion if it has expired (or will expire soon).
         */
        private void removeIfExpired() {
            synchronized (tokenServerSsg) {
                if (cachedAssertion != null && cachedAssertion.isExpiringSoon(SAML_PREEXPIRE_SEC)) {
                    log.log(Level.INFO, "Our SAML Holder-of-key assertion has expired or will do so within the next " +
                                        SAML_PREEXPIRE_SEC + " seconds.  Will throw it away and get a new one.");
                    cachedAssertion = null;
                }
            }
        }

        public void onTokenRejected() {
            synchronized (tokenServerSsg) {
                cachedAssertion = null;
            }
        }

        private SamlAssertion acquireSamlAssertion()
                throws OperationCanceledException, GeneralSecurityException,
                KeyStoreCorruptException, BadCredentialsException, IOException
        {
            log.log(Level.INFO, "Applying for SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
            SamlAssertion s;
            // TODO extract the strategies for getting tokenServer client cert, private key, and server cert
            s = TokenServiceClient.obtainSamlAssertion(tokenServerSsg,
                                                       SsgKeyStoreManager.getClientCert(tokenServerSsg),
                                                       SsgKeyStoreManager.getClientCertPrivateKey(tokenServerSsg),
                                                       SsgKeyStoreManager.getServerCert(tokenServerSsg),
                                                       TokenServiceClient.RequestType.ISSUE);
            log.log(Level.INFO, "Obtained SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
            return s;
        }
    }
}
