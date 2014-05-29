package com.l7tech.proxy.datamodel;

import com.l7tech.common.http.FailoverHttpClient;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import static com.l7tech.common.io.failover.AbstractFailoverStrategy.makeSynchronized;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.io.failover.StickyFailoverStrategy;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.ssl.*;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.DateTranslator;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.collections.map.LRUMap;
import org.apache.http.conn.ClientConnectionManager;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds SSG behaviour, strategies, and transient state.
 */
@SuppressWarnings({"JavaDoc"})
public class SsgRuntime {
    private static final Logger log = Logger.getLogger(SsgRuntime.class.getName());

    private final Ssg ssg;

    public static final String SSGPROP_STRIPHEADER = "response.security.stripHeader"; // "always", "when_processed", "secure_span", or "lazy"; default "always"
    public static final String SSGPROP_DEFAULT_SECURITY_ACTOR = "wss.decorator.defaultSecurityActor";
    public static final String SSGPROP_SECURITY_MUSTUNDERSTAND = "wss.decorator.mustUnderstand";

    // True if there is a pending request to flush the global SSL session context
    private static final AtomicBoolean sslSessionFlushRequired = new AtomicBoolean(false);

    // Maximum simultaneous outbound connections.  Throttled wide-open.
    public static final int MAX_CONNECTIONS = 60000;

    // Maximum number of times we should show a logon dialog if the user hits cancel.
    // note that the dialog is disabled AFTER this many displays (+1, since we start at 0)
    public static final int MAX_LOGON_CANCEL = 1;

    // Maximum number of Sender Vouches tokens to cache at a time.
    private static final int MAX_SV_USERS = ConfigFactory.getIntProperty( "com.l7tech.proxy.maxSvUsers", 1000 );

    private ClientConnectionManager httpConnectionManager;

    private SsgNotifyPolicyManager ssgNotifyPolicyManager = null; // policy store that is not saved to disk
    private char[] password = null;
    private boolean promptForUsernameAndPassword = true;
    private KeyStore keyStore = null;
    private KeyStore trustStore = null;
    private Boolean haveClientCert = null;
    private int numTimesLogonDialogCanceled = 0;
    private long credentialsUpdatedTimeMillis = 0;
    private PrivateKey privateKey = null; // cache of private key
    private byte[] privateKeyPasswordHash = null; // cache of private key
    private SSLContext sslContext = null;
    private X509TrustManager trustManager = null;
    private HttpCookie[] sessionCookies = null;
    private X509Certificate serverCert = null;
    private X509Certificate clientCert = null;
    private byte[] secureConversationSharedSecret = null;
    private String secureConversationId = null;
    private Calendar secureConversationExpiryDate = null;
    private KerberosServiceTicket kerberosTicket = null;
    private long timeOffset = 0;
    private Map<SecurityTokenType, TokenStrategy> tokenStrategiesByType;
    private Map<List<Object>, TokenStrategy> senderVouchesTokenStrategiesByUser;
    private SimpleHttpClient simpleHttpClient = null;
    private CredentialManager credentialManager = null;
    private SsgKeyStoreManager ssgKeyStoreManager;
    private String policyServiceFile = SecureSpanConstants.POLICY_SERVICE_FILE;

    private DateTranslator fromSsgDateTranslator = new DateTranslator() {
        protected long getOffset() {
            return -getTimeOffset();
        }

        @Override
        protected void log(long source, Date result) {
            if (log.isLoggable(Level.FINE))
                log.fine("Translating date from SSG clock " + new Date(source) + " to local clock " + result);
        }
    };

    private DateTranslator toSsgDateTranslator = new DateTranslator() {
        protected long getOffset() {
            return getTimeOffset();
        }

        @Override
        protected void log(long source, Date result) {
            if (log.isLoggable(Level.FINE))
                log.fine("Translating date from local clock " + source + " to SSG clock " + result);
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
        }
    }

    /**
     * @return the root PolicyManager for this SSG.  Never null.
     */
    public PolicyManager getPolicyManager() {
        if (ssgNotifyPolicyManager == null) {
            synchronized (ssg) {
                if (ssgNotifyPolicyManager == null) {
                    ssgNotifyPolicyManager = new SsgNotifyPolicyManager(ssg, new TransientPolicyManager(ssg.getPersistentPolicyManager()));
                }
            }
        }
        return ssgNotifyPolicyManager;
    }

    /**
     * Replace the root policy manager.
     * This is not called in the stand-alone SSB, but is used by
     * the {@link com.l7tech.proxy.SecureSpanBridge} implementation when a hardcoded policy is selected.
     */
    public void setPolicyManager(PolicyManager p) {
        ssgNotifyPolicyManager = new SsgNotifyPolicyManager(ssg, p);
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

    /**
     * Get the Kerberos ticket if set.
     */
    public KerberosServiceTicket kerberosTicket() {
        return kerberosTicket;
    }

    /**
     * Set the Kerberos ticket.
     */
    public void kerberosTicket(KerberosServiceTicket kerberosTicket) {
        this.kerberosTicket = kerberosTicket;
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

    /** Transient cache of private key password hash  */
    byte[] getPrivateKeyPasswordHash() {
        return privateKeyPasswordHash;
    }

    /** Transient cache of private key password hash */
    void setPrivateKeyPasswordHash(byte[] privateKeyPasswordHash) {
        this.privateKeyPasswordHash = privateKeyPasswordHash;
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

    private Map<SecurityTokenType, TokenStrategy> getTokenStrategiesByType() {
        synchronized (ssg) {
            if (tokenStrategiesByType == null) {
                tokenStrategiesByType = new HashMap<SecurityTokenType, TokenStrategy>();
                if (ssg.isGeneric()) // Generic account does not support any security token strategies
                    return tokenStrategiesByType;
                TokenStrategy samlStrat1 = ssg.getWsTrustSamlTokenStrategy();
                TokenStrategy samlStrat2 = ssg.getWsTrustSamlTokenStrategy();
                if (samlStrat1 == null) {
                    Ssg tokenServerSsg = ssg.getTrustedGateway();
                    if (tokenServerSsg == null) tokenServerSsg = ssg;
                    samlStrat1 = new TrustedSsgSamlTokenStrategy(tokenServerSsg, 1);
                    samlStrat2 = new TrustedSsgSamlTokenStrategy(tokenServerSsg, 2);
                }
                tokenStrategiesByType.put(SecurityTokenType.SAML_ASSERTION, samlStrat1);
                if (samlStrat2 != null) {
                    tokenStrategiesByType.put(SecurityTokenType.SAML2_ASSERTION, samlStrat2);
                }
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
            return getTokenStrategiesByType().get(tokenType);
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

    /**
     * Get the strategy for obtaining a SAML sender-vouches assertion vouching for the specified username.
     *
     * @param tokenType the type of SecurityTokenType that is required
     * @param username  the username to vouch for (if a String), or disambiguation object if falling back to generic strategy
     * @param nameIdFormatUri an overridden NameIdentifier Format URI (null to base on credentials)
     * @param authnMethodUri an overridden Authentication Method Format URI (null to base on credentials)
     * @param nameIdTemplate a {@link java.text.MessageFormat} template to use in generating NameIdentifier values (null to use the name alone)
     * @return a strategy that will produce a token for this user.  Never null.
     */
    public TokenStrategy getSenderVouchesStrategyForUsername(SecurityTokenType tokenType,
                                                             Object username,
                                                             String nameIdFormatUri,
                                                             String authnMethodUri,
                                                             String nameIdTemplate)
    {
        synchronized (ssg) {
            if (senderVouchesTokenStrategiesByUser == null) {
                //noinspection unchecked
                senderVouchesTokenStrategiesByUser = new LRUMap(MAX_SV_USERS);
            }

            List<Object> keyList = new ArrayList<Object>();
            keyList.add(tokenType);
            keyList.add(username);
            if (nameIdFormatUri != null) keyList.add(nameIdFormatUri);
            if (authnMethodUri != null) keyList.add(authnMethodUri);
            if (nameIdTemplate != null) keyList.add(nameIdTemplate);

            TokenStrategy strat = senderVouchesTokenStrategiesByUser.get(keyList);
            if (strat == null && username instanceof String) {
                strat = new SenderVouchesSamlTokenStrategy(tokenType, nameIdFormatUri, (String)username, nameIdTemplate, authnMethodUri);
                senderVouchesTokenStrategiesByUser.put(keyList, strat);
            } else if (!(username instanceof String)) {
                strat = getTokenStrategy(tokenType);
                senderVouchesTokenStrategiesByUser.put(keyList, strat);
            }
            return strat;
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
            final ClientProxyKeyManager keyManager = SslInstanceHolder.keyManager;
            final X509TrustManager trustManager = getTrustManager();
            try {
                sslContext = CurrentSslPeer.doWithSslPeer(ssg, new Callable<SSLContext>() {
                    @Override
                    public SSLContext call() throws Exception {
                        String tlsProv = ConfigFactory.getProperty( SslPeer.PROP_SSL_PROVIDER );
                        SSLContext sslContext = tlsProv == null ? SSLContext.getInstance("TLS") : SSLContext.getInstance("TLS", tlsProv);
                        sslContext.init(new X509KeyManager[] {keyManager},
                                        new X509TrustManager[] {trustManager},
                                        null);
                        return sslContext;
                    }
                });
                maybeFlushAllSslSessions(sslContext);
                return sslContext;
            } catch (RuntimeException e) {
                if (ExceptionUtils.causedBy(e, CredentialsRequiredException.class))
                    throw e;
                log.log(Level.SEVERE, "Unable to create SSL context", e);
                throw new RuntimeException(e); // shouldn't happen
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to create SSL context", e);
                throw new RuntimeException(e); // shouldn't happen
            }
        }
    }

    private static void maybeFlushAllSslSessions(SSLContext sslContext) {
        if (sslSessionFlushRequired.getAndSet(false)) {
            flushAllSslSessions(sslContext);
        }
    }

    public SSLContext getSslContext() {
        synchronized (ssg) {
            if (sslContext == null)
                sslContext = createSslContext();
            return sslContext;
        }
    }

    /** Notify that this SsgRuntime instance is about to be thrown away. */
    void close() {
        resetSslContext();
    }

    private static void flushAllSslSessions(SSLContext sslContext) {
        SSLSessionContext sc = sslContext.getClientSessionContext();
        if (sc == null)
            return;
        List<byte[]> idlist = new ArrayList<byte[]>();
        Enumeration<byte[]> ids = sc.getIds();
        while (ids.hasMoreElements()) {
            idlist.add(ids.nextElement());
        }
        for (byte[] bytes : idlist) {
            if (bytes != null) {
                SSLSession sess = sc.getSession(bytes);
                if (sess != null)
                    sess.invalidate();
            }
        }
    }

    /** Flush SSL context and cached certificates. */
    public void resetSslContext()
    {
        synchronized (ssg) {
            sslSessionFlushRequired.set(true);
            keyStore(null);
            trustStore(null);
            setCachedPrivateKey(null);
            setPrivateKeyPasswordHash(null);
            setHaveClientCert(null);
            setCachedClientCert(null);
            setCachedServerCert(null);
            clearSessionCookies();
            sslContext = null;
            serverCert = null;
            clientCert = null;
            simpleHttpClient = null;
            httpConnectionManager = null; // bugzilla #1808
        }
        ssg.fireSsgEvent(SsgEvent.createSslResetEvent(ssg)); // bug #2540
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

    public ClientConnectionManager getHttpConnectionManager() {
        synchronized (ssg) {
            if (httpConnectionManager == null) {
                httpConnectionManager = HttpComponentsClient.newConnectionManager();
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
                GenericHttpClient client = new HttpComponentsClient(getHttpConnectionManager(), ssg.getHttpConnectTimeout(), ssg.getHttpReadTimeout());

                // Make it use the right SSL
                client = new SslPeerHttpClient(client,
                                               ssg,
                                               new SslPeerLazyDelegateSocketFactory(ssg));

                // Add failover if so configured.
                // Failover must be added last so requests can be downcast to FailoverHttpRequest in MP,
                // so an InputStreamFactory can be set; otherwise all requests are buffered.
                final String[] addrs = ssg.getOverrideIpAddresses();
                if (ssg.isUseOverrideIpAddresses() && addrs != null && addrs.length > 0) {
                    log.fine("Enabling failover IP list for Gateway " + ssg);
                    FailoverStrategy<String> strategy;
                    try {
                        strategy = FailoverStrategyFactory.createFailoverStrategy(ssg.getFailoverStrategyName(), addrs);
                    } catch (IllegalArgumentException e) {
                        strategy = new StickyFailoverStrategy<String>(addrs);
                    }
                    int max = addrs.length;
                    client = new FailoverHttpClient(client, makeSynchronized( strategy ), max, log);
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

    /**
     * @param policyServiceFile the new policy service file for this SSG, ie "/ssg/policy/disco".
     */
    public void setPolicyServiceFile(String policyServiceFile) {
        if (policyServiceFile == null) throw new NullPointerException();
        this.policyServiceFile = policyServiceFile;
    }

    /**
     * @return the policy service file for this SSG, either "/ssg/policy/disco" or "/ssg/policy/disco.modulator"
     *         if this SSG has been reported to be pre-3.2.
     */
    public String getPolicyServiceFile() {
        return policyServiceFile;
    }

    /**
     * @return true if SOAP responses are always run through WSS processor soon after they are received.
     *         false if the WSS processor is not invoked on the response unless a client assertion attempts
     *               to access the response processor results.
     */
    public boolean isEagerToProcessResponseSecurity() {
        final String val = ssg.getProperties().get(SSGPROP_STRIPHEADER);
        return val == null ||
               "always".equals(val) ||
               "secure_span".equals(val);
    }

    /**
     * @return true if a response's processed Security header is only stripped if it was explicitly
     *               addressed to the Layer 7 actor name or URI. 
     *         false if a response's processed Security header is always stripped before the response
     *               is returned to the client.
     */
    public boolean isReluctantToRemoveProcessedSecurityHeader() {
        final String stripMode = ssg.getProperties().get(SSGPROP_STRIPHEADER);
        return "secure_span".equals(stripMode) || "lazy".equals(stripMode);
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
        if (credentialManager == null) {
            if (ssg.isChainCredentialsFromClient()) {
                // Use special credential manager that triggers HTTP challenge back to the client
                // any time (new) credentials are needed
                credentialManager = new DelegatingCredentialManager(Managers.getCredentialManager()) {
                    public PasswordAuthentication getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws HttpChallengeRequiredException {
                        throw new HttpChallengeRequiredException("invalid username or password");
                    }

                    public PasswordAuthentication getCredentialsWithReasonHint(Ssg ssg, ReasonHint hint, boolean disregardExisting, boolean reportBadPassword) throws HttpChallengeRequiredException {
                        throw new HttpChallengeRequiredException("username and password required");
                    }

                    public PasswordAuthentication getCredentials(Ssg ssg) throws HttpChallengeRequiredException {
                        throw new HttpChallengeRequiredException("username and password required");
                    }
                };
            } else {
                credentialManager = Managers.getCredentialManager();
            }
        }

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

    public String toString() {
        return "SsgRuntime:" + ssg.toString();
    }
}
