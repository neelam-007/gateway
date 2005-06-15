/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.http.FailoverHttpClient;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.DateTranslator;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.*;
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

    // Maximum simultaneous outbound connections.  Throttled wide-open.
    public static final int MAX_CONNECTIONS = 60000;

    private MultiThreadedHttpConnectionManager httpConnectionManager;

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
    private SSLContext sslContext = null;
    private X509TrustManager trustManager = null;
    private HttpCookie[] sessionCookies = null;
    private X509Certificate serverCert = null;
    private X509Certificate clientCert = null;
    private byte[] secureConversationSharedSecret = null;
    private String secureConversationId = null;
    private Calendar secureConversationExpiryDate = null;
    private long timeOffset = 0;
    private Map tokenStrategiesByType;
    private SimpleHttpClient simpleHttpClient = null;
    private CredentialManager credentialManager = Managers.getCredentialManager();
    private SsgKeyStoreManager ssgKeyStoreManager;

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
        this.ssgKeyStoreManager = new Pkcs12SsgKeyStoreManager(ssg);

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
    public PolicyManager getPolicyManager() {
        if (rootPolicyManager == null) {
            synchronized (ssg) {
                if (rootPolicyManager == null) {
                    rootPolicyManager = new TransientPolicyManager(ssg.getPersistentPolicyManager());
                }
            }
        }
        return rootPolicyManager;
    }

    /**
     * Replace the root policy manager.
     * This is not called in the stand-alone SSB, but is used by 
     * the {@link com.l7tech.policy.assertion.BridgeRoutingAssertion},
     * and the {@link com.l7tech.proxy.SecureSpanBridge} implementation, when a hardcoded policy is selected.
     */
    public void setPolicyManager(PolicyManager p) {
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
        if (username != null && ssg.isFederatedGateway())
            return new PasswordAuthentication(username, new char[0]); // shield actual password in federated case
        if (username != null && username.length() > 0 && password != null)
            return new PasswordAuthentication(username, password);
        return null;
    }

    private Map getTokenStrategiesByType() {
        synchronized (ssg) {
            if (tokenStrategiesByType == null) {
                tokenStrategiesByType = new HashMap();
                TokenStrategy samlStrat = ssg.getWsTrustSamlTokenStrategy();
                if (samlStrat == null) {
                    Ssg tokenServerSsg = ssg.getTrustedGateway();
                    if (tokenServerSsg == null) tokenServerSsg = ssg;
                    samlStrat = new TrustedSsgSamlTokenStrategy(tokenServerSsg);
                }
                tokenStrategiesByType.put(SecurityTokenType.SAML_ASSERTION, samlStrat);
            }
            return tokenStrategiesByType;
        }
    }

    /**
     * Get the strategy for obtaining a specific type of security token.
     *
     * @param tokenType the type of security token to obtain.  Must not be null
     * @return the strategy for getting the requested security token for this Ssg, or null if this Ssg
     *          is not configured with a strategy for the requested token type.
     */
    public TokenStrategy getTokenStrategy(SecurityTokenType tokenType) {
        if (tokenType == null) throw new NullPointerException();
        synchronized (ssg) {
            return (TokenStrategy)getTokenStrategiesByType().get(tokenType);
        }
    }

    /**
     * Set the strategy for obtaining a specific type of security token.
     *
     * @param tokenType the type of security token to strategise.  Must not be null
     * @param strategy the new strategy to use. Must not be null
     */
    public void setTokenStrategy(SecurityTokenType tokenType, TokenStrategy strategy) {
        if (tokenType == null || strategy == null) throw new NullPointerException();
        synchronized (ssg) {
            getTokenStrategiesByType().put(tokenType, strategy);
        }
    }

    /** Get the trust manager used for SSL connections to this SSG. */
    public X509TrustManager getTrustManager() {
        synchronized (ssg) {
            if (trustManager == null) {
                trustManager = SslInstanceHolder.trustManager;
            }
            return trustManager;
        }
    }

    /** Set the trust manager to use for SSL connections to this SSG. */
    public void setTrustManager(X509TrustManager tm) {
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
            X509TrustManager trustManager = getTrustManager();
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSL", System.getProperty(SslPeer.PROP_SSL_PROVIDER,
                                                                              SslPeer.DEFAULT_SSL_PROVIDER));
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
            simpleHttpClient = null;
            httpConnectionManager = null; // bugzilla #1808
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
     * @return  HttpCookie[]  The list of session cookies.
     */
    public HttpCookie[] getSessionCookies() {
        synchronized (ssg) {
            return sessionCookies;
        }
    }

    /**
     * Get the cookies as a string.
     *
     * @return a string like "foo=bar; baz=blat; bloo=blot".  May be empty, but never null.
     */
    public String getSessionCookiesHeaderValue() {
        StringBuffer sb = new StringBuffer();
        HttpCookie[] sessionCookies = getSessionCookies();
        if (sessionCookies == null)
            return "";
        for (int i = 0; i < sessionCookies.length; i++) {
            if (i > 0)
                sb.append("; ");
            HttpCookie cook = sessionCookies[i];
            sb.append(cook.toExternalForm());
        }
        return sb.toString();
    }

    // TODO remove this if it isn't needed
    private static void logSessionCookies(HttpCookie[] sessionCookies) {
            String cookieString = "HTTP cookies: ";
            if (sessionCookies != null && sessionCookies.length > 0) {

                for (int i = 0; i < sessionCookies.length; i++) {
                    cookieString += "[ " + sessionCookies[i].toExternalForm() + " ], ";
                }
            } else {
                cookieString += " empty";
            }

            log.info(cookieString);
    }

    /**
     * Store the HTTP cookies of the user session established with SiteMinder Policy Server.
     *
     * @param cookies  The HTTP cookies to be saved.
     */
    public void setSessionCookies(HttpCookie[] cookies) {
        synchronized (ssg) {
            sessionCookies = cookies;
        }
    }

    /**
     * Delete the cookies.
     */
    public void clearSessionCookies() {
        synchronized (ssg) {
            sessionCookies = new HttpCookie[0];
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
        synchronized (ssg) {
            if (httpConnectionManager == null) {
                httpConnectionManager = new MultiThreadedHttpConnectionManager();
                httpConnectionManager.setMaxConnectionsPerHost(MAX_CONNECTIONS);
                httpConnectionManager.setMaxTotalConnections(MAX_CONNECTIONS);
            }
            return httpConnectionManager;
        }
    }

    /**
     * Get a GenericHttpClient ready to make a single request to this Ssg.
     * Currently this HTTP client is not very performant and should not be used in performance-critical sections of code.
     * @return the HTTP client.  Never null.
     */
    public SimpleHttpClient getHttpClient() {
        synchronized (ssg) {
            if (simpleHttpClient == null) {
                // Base it on commons
                GenericHttpClient client = new CommonsHttpClient(getHttpConnectionManager());

                // Make it use the right SSL
                client = new SslPeerHttpClient(client,
                                               ssg,
                                               ClientProxySecureProtocolSocketFactory.getInstance());

                // Add failover if so configured.
                // Failover must be added last so requests can be downcast to FailoverHttpRequest in MP,
                // so an InputStreamFactory can be set; otherwise all requests are buffered.
                final String[] addrs = ssg.getOverrideIpAddresses();
                if (ssg.isUseOverrideIpAddresses() && addrs != null && addrs.length > 0) {
                    log.fine("Enabling failover IP list for Gateway " + ssg);
                    FailoverStrategy strategy;
                    try {
                        strategy = FailoverStrategyFactory.createFailoverStrategy(ssg.getFailoverStrategyName(), addrs);
                    } catch (IllegalArgumentException e) {
                        strategy = new StickyFailoverStrategy(addrs);
                    }
                    int max = addrs.length;
                    client = new FailoverHttpClient(client, strategy, max, log);
                }

                // (SimpleHttpClient passes-through requests, so it won't wrap FailoverHttpRequest instances.)
                simpleHttpClient = new SimpleHttpClient(client);
            }
            return simpleHttpClient;
        }
    }

    /**
     * Set the SimpleHttpClient to use for requests to this Ssg.
     * @param httpClient the new HTTP client, or null to use the default one.
     */
    public void setHttpClient(SimpleHttpClient httpClient) {
        simpleHttpClient = httpClient;
    }

    // Hack to allow lazy initialization of SSL stuff
    private static class SslInstanceHolder {
        private static final ClientProxyKeyManager keyManager = new ClientProxyKeyManager();
        private static final ClientProxyTrustManager trustManager = new ClientProxyTrustManager();
    }

    public void handleKeyStoreCorrupt() throws OperationCanceledException {
        Ssg problemSsg = ssg.getTrustedGateway();
        if (problemSsg == null) problemSsg = ssg;
        problemSsg.getRuntime().getCredentialManager().notifyKeyStoreCorrupt(problemSsg);
        problemSsg.getRuntime().getSsgKeyStoreManager().deleteStores();
        resetSslContext();
    }

    public void discoverServerCertificate(PasswordAuthentication pw)
            throws GeneralSecurityException, IOException, BadCredentialsException
    {
        for (;;) {
            try {
                log.info("Attempting to discover Gateway server certificate for " + this);
                getSsgKeyStoreManager().installSsgServerCertificate(ssg, pw);
                return;
            } catch (OperationCanceledException e) {
                throw new RuntimeException(e); // cancel it
            } catch (KeyStoreCorruptException e) {
                try {
                    handleKeyStoreCorrupt();
                } catch (OperationCanceledException e1) {
                    throw new RuntimeException(e1);
                }
                /* FALLTHROUGH and retry */
            }
        }
    }

    /**
     * @return the Credential Manager to use for this Ssg.  Never null.
     */
    public CredentialManager getCredentialManager() {
        return credentialManager;
    }

    /**
     * @param credentialManager the Credential Manager to use for this Ssg.  Must not be null.
     */
    public void setCredentialManager(CredentialManager credentialManager) {
        if (credentialManager == null) throw new NullPointerException();
        this.credentialManager = credentialManager;
    }

    /**
     * @return the SsgKeyStoreManager to use for managing persistence of key and cert material for this Ssg.  Never null.
     */
    public SsgKeyStoreManager getSsgKeyStoreManager() {
        return ssgKeyStoreManager;
    }

    /**
     * @param ssgKeyStoreManager to use for managing persistence of key and cert material for this Ssg.  Must not be null.
     */
    public void setSsgKeyStoreManager(SsgKeyStoreManager ssgKeyStoreManager) {
        if (ssgKeyStoreManager == null) throw new NullPointerException();
        this.ssgKeyStoreManager = ssgKeyStoreManager;
    }
}
