package com.l7tech.proxy.datamodel;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.DateTranslator;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.ssl.ClientProxyKeyManager;
import com.l7tech.proxy.ssl.ClientProxyTrustManager;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-core representation of an SSG.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:09:04 AM
 */
public class Ssg implements Serializable, Cloneable, Comparable {
    private static final Logger log = Logger.getLogger(Ssg.class.getName());
    private static final String SSG_PROTOCOL = "http";
    private static final int SSG_SSL_PORT = 8443;
    private static final int SSG_PORT = 8080;

    // The file that contains our client cert private key for this Ssg
    private static final String KEY_FILE = ClientProxy.PROXY_CONFIG + File.separator + "key";
    private static final String KEY_EXT = ".p12";

    // The file that contains the trusted server cert for this Ssg
    private static final String TRUST_FILE = ClientProxy.PROXY_CONFIG + File.separator + "certs";
    private static final String TRUST_EXT = ".p12";

    // Maximum simultaneous outbound connections.  Throttled wide-open.
    public static final int MAX_CONNECTIONS = 60000;

    // Hack to allow lazy initialization of SSL stuff
    private static class SslInstanceHolder {
        private static final ClientProxyKeyManager keyManager = new ClientProxyKeyManager();
        private static final ClientProxyTrustManager trustManager = new ClientProxyTrustManager();
    }

    private long id = 0;
    private Ssg trustedGateway = null;
    private String localEndpoint;
    private String ssgAddress = "";
    private int ssgPort = SSG_PORT;
    private String ssgFile = SecureSpanConstants.SSG_FILE;
    private int sslPort = SSG_SSL_PORT;
    private String username = null;
    private String keyStorePath = null;
    private String trustStorePath = null;
    private boolean defaultSsg = false;
    private boolean chainCredentialsFromClient = false;
    private boolean useSslByDefault = true;
    private boolean savePasswordToDisk = false;
    private byte[] persistPassword = null;
    private boolean useOverrideIpAddresses = false;
    private String[] overrideIpAddresses = null;
    private LocalPolicyManager persistentPolicyManager = new LocalPolicyManager(); // policy store that gets saved to disk

    // These fields are transient.  To prevent the bean serializer from saving them anyway,
    // they do not use the getFoo() / setFoo() naming convention in their accessors and mutators.
    private transient LocalPolicyManager rootPolicyManager = null; // policy store that is not saved to disk
    private transient char[] password = null;
    private transient boolean promptForUsernameAndPassword = true;
    private transient KeyStore keyStore = null;
    private transient KeyStore trustStore = null;
    private transient Boolean haveClientCert = null;
    private transient int numTimesLogonDialogCanceled = 0;
    private transient long credentialsUpdatedTimeMillis = 0;
    private transient Set listeners = new HashSet(); // List of weak references to listeners
    private transient PrivateKey privateKey = null; // cache of private key
    private transient boolean passwordWorkedForPrivateKey = false;
    private transient boolean passwordWorkedWithSsg = false;
    private transient SSLContext sslContext = null;
    private transient ClientProxyTrustManager trustManager = null;
    private transient Cookie[] sessionCookies = null;
    private transient X509Certificate serverCert = null;
    private transient X509Certificate clientCert = null;
    private transient byte[] secureConversationSharedSecret = null;
    private transient String secureConversationId = null;
    private transient Calendar secureConversationExpiryDate = null;
    private transient SamlHolderOfKeyAssertion samlHolderOfKeyAssertion = null;
    private transient long timeOffset = 0;

    private transient final MultiThreadedHttpConnectionManager httpConnectionManager = new MultiThreadedHttpConnectionManager();

    public int compareTo(final Object o) {
        long id0 = getId();
        long id1 = ((Ssg)o).getId();
        if (id0 == 0 || id1 == 0)
            throw new IllegalArgumentException("Comparison of Ssgs without Ids is not defined");
        return id0 < id1 ? -1 : id0 > id1 ? 1 : 0;
    }

    public boolean equals(final Object o) {

        if(o == null) return false;
        
        if (this == o)
            return true;
        if (!o.getClass().equals(this.getClass()))
            return false;
        if (getId() == 0 || ((Ssg)o).getId() == 0)
            return false;
        return getId() == ((Ssg)o).getId();
    }

    public int hashCode() {
        return (int)getId();
    }

    /** Create a new Ssg instance with default fields. */
    public Ssg() {
        this.httpConnectionManager.setMaxConnectionsPerHost(MAX_CONNECTIONS);
        this.httpConnectionManager.setMaxTotalConnections(MAX_CONNECTIONS);
    }

    /** Create a new Ssg instance with the given ID. */
    public Ssg(long id) {
        this();
        this.id = id;
    }

    /**
     * Create a new Ssg instance with the given field contents.
     * @param id        assigned by the SsgManager.  Unique number for identifying this Ssg instance.
     * @param serverAddress hostname or address of the associated SSG.
     */
    public Ssg(long id, final String serverAddress) {
        this(id);
        this.localEndpoint = null;
        this.ssgAddress = serverAddress;
    }

    public String toString() {
        return getSsgAddress();
    }

    /**
     * Add a listener to be notified of changes to this Ssg's state, including policy changes.
     * Adding a listener requires that a Gui be available, since event delivery is done on the Swing
     * thread.  The listeners are stored in a Set; thus you may add the same listener instance multiple
     * times, but it will only receive one copy of each event.
     */
    public synchronized void addSsgListener(SsgListener listener) {
        listeners.add(new WeakReference(listener));
    }

    /**
     * Remove a listener from the queue.
     */
    public synchronized void removeSsgListener(SsgListener listener) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            WeakReference reference = (WeakReference) i.next();
            Object o = reference.get();
            if (o == null || o == listener)
                i.remove();
        }
    }

    /**
     * Notify all listeners that an SsgEvent has occurred.
     * @param event  the event to transmit
     */
    private void fireSsgEvent(final SsgEvent event) {
        if (event.getSource() != this)
            throw new IllegalArgumentException("Event to be fired must identify this Gateway as the source");
        if (listeners.size() < 1)
            return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for (Iterator i = listeners.iterator(); i.hasNext();) {
                    WeakReference reference = (WeakReference) i.next();
                    SsgListener target = (SsgListener) reference.get();
                    if (target != null) {
                        if (event.getType() == SsgEvent.POLICY_ATTACHED)
                            target.policyAttached(event);
                        else if (event.getType() == SsgEvent.DATA_CHANGED)
                            target.dataChanged(event);
                        else
                            throw new IllegalArgumentException("Unknown event type: " + event.getType());
                    }
                }
            }
        });
    }

    /** Fire a new DATA_CHANGED event. */
    private void fireDataChangedEvent() {
        fireSsgEvent(SsgEvent.createDataChangedEvent(this));
    }

    /**
     * @return the root PolicyManager for this SSG.  Never null.
     */
    public LocalPolicyManager rootPolicyManager() {
        if (rootPolicyManager == null) {
            synchronized (this) {
                if (rootPolicyManager == null) {
                    rootPolicyManager = new LocalPolicyManager(getPersistentPolicyManager());
                }
            }
        }
        return rootPolicyManager;
    }

    /** Replace the root policy manager.  This should never be called by a production class; it is here only for test purposes. */
    public void rootPolicyManager(LocalPolicyManager p) {
        rootPolicyManager = p;
    }

    /**
     * Get the PolicyManager whose cached policies are saved to disk.  Should be used only by GUI code
     * and the bean serializer.
     *
     * @return the persistent policy manager.  Never null.
     */
    public PolicyManager getPersistentPolicyManager() {
        return persistentPolicyManager;
    }

    /** @deprecated Needed for bean serializer only; do not use. */
    public void setPersistentPolicyManager(LocalPolicyManager persistentPolicyManager) {
        if (persistentPolicyManager == null) persistentPolicyManager = new LocalPolicyManager(); // just in case
        this.persistentPolicyManager = persistentPolicyManager;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get the "Trusted Gateway" for this Ssg.  The "Trusted Gateway" is used to obtain authentication information
     * instead of getting it directly from this Ssg.
     * @return
     */
    public Ssg getTrustedGateway() {
        return trustedGateway;
    }

    public void setTrustedGateway(Ssg trustedGateway) {
        this.trustedGateway = trustedGateway;
    }

    public String getLocalEndpoint() {
        if (localEndpoint == null)
            localEndpoint = "gateway" + getId();
        return localEndpoint;
    }

    public void setLocalEndpoint(String localEndpoint) {
        if (localEndpoint == null)
            throw new IllegalArgumentException("localEndpoint may not be null");
        if (localEndpoint.length() > 1 && "/".equals(localEndpoint.substring(0, 1)))
            localEndpoint = localEndpoint.substring(1);
        this.localEndpoint = localEndpoint;
    }

    public String getSsgAddress() {
        return ssgAddress;
    }

    public void setSsgAddress(final String ssgAddress) {
        if (ssgAddress == null)
            throw new IllegalArgumentException("ssgAddress may not be null");
        if (ssgAddress.length() < 1)
            throw new IllegalArgumentException("ssgAddress may not be the empty string");
        this.ssgAddress = ssgAddress;
    }

    public int getSsgPort() {
        return ssgPort;
    }

    public void setSsgPort(int ssgPort) {
        this.ssgPort = ssgPort;
    }

    public URL getServerUrl() {
        URL url = null;
        try {
            url = new URL(SSG_PROTOCOL, getSsgAddress(), getSsgPort(), getSsgFile());
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                log.log(Level.SEVERE, "This can't have happened", e);
                return null; // totally can't happen
            }
        }
        return url;
    }

    public URL getServerSslUrl() {
        URL url = null;
        try {
            url = new URL("https", getSsgAddress(), getSslPort(), getSsgFile());
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, "Unable to build Gateway SSL URL", e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                log.log(Level.SEVERE, "This can't have happened", e);
                return null; // totally can't happen
            }
        }
        return url;
    }

    public URL getServerPasswordChangeUrl() {
        URL url = null;
        try {
            url = new URL("https", getSsgAddress(), getSslPort(), SecureSpanConstants.PASSWD_SERVICE_FILE);
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, "Unable to build valid URL for Gateway's password changing service", e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                throw new RuntimeException(e1); // can't happen
            }
        }
        return url;
    }

    public URL getServerCertificateSigningRequestUrl() {
        URL url = null;
        try {
            url = new URL("https", getSsgAddress(), getSslPort(), SecureSpanConstants.CERT_REQUEST_FILE);
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, "Unable to build valid URL for Gateway's certificate signing service", e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                throw new RuntimeException(e1); // can't happen
            }
        }
        return url;
    }

    /**
     * Check if a non-null and non-empty username, and a non-null password, are configured for this SSG.
     * @return
     */
    /*public boolean isCredentialsConfigured() {
        return getUsername() != null && password() != null && getUsername().length() > 0;
    }*/

    public String getSsgFile() {
        return ssgFile;
    }

    public void setSsgFile(String ssgFile) {
        if (ssgFile == null)
            throw new IllegalArgumentException("ssgFile may not be null");
        this.ssgFile = ssgFile;
    }

    /**
     * Get the protocol used to talk to the SSG.
     * @return Returns "http".
     */
    static String getSsgProtocol() {
        return SSG_PROTOCOL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;

        // clear session cookies when a user name is changed/set
        clearSessionCookies();

        fireDataChangedEvent();
    }

    /** Obfuscate the password for storage to disk in plaintext. */
    private byte[] obfuscatePassword(char[] password) {
        if (password == null)
            return null;

        String obfuscated = HexUtils.encodeBase64(new String(password).getBytes());
        return obfuscated.getBytes();
    }

    /** De-obfuscate a password that was stored to disk in plaintext. */
    private char[] deobfuscatePassword(byte[] persistPassword) {
        if (persistPassword == null)
            return null;

        try {
            return new String(HexUtils.decodeBase64(new String(persistPassword))).toCharArray();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to recover persisted password", e);
            return null;
        }
    }

    /**
     * Read the in-memory cached password for this SSG. Should be used only by a CredentialManager
     * or the SsgPropertiesDialog.
     * Others should use the CredentialManager's getCredentialsForTrustedSsg() method instead.
     *
     * @return the password, or null if we don't have one in memory for this SSG
     */
    public char[] cmPassword() {
        if (password == null)
            password = deobfuscatePassword(getPersistPassword());
        return password;
    }

    /**
     * Set the in-memory cached password for this SSG.  Should be used only by a CredentialManager
     * or the SsgPropertyDialog.
     * Others should use the CredentialManager's getNewCredentials() method instead.
     *
     * @param password
     */
    public void cmPassword(final char[] password) {
        if (this.password != password) {
            this.passwordWorkedForPrivateKey = false;
            this.passwordWorkedWithSsg = false;
        }
        this.password = password;

        // clear session cookies when a user name is changed/set
        clearSessionCookies();

        if (isSavePasswordToDisk())
            setPersistPassword(obfuscatePassword(password));
        else
            setPersistPassword(null);
    }

    public boolean isSavePasswordToDisk() {
        return savePasswordToDisk;
    }

    public void setSavePasswordToDisk(boolean savePasswordToDisk) {
        this.savePasswordToDisk = savePasswordToDisk;
        cmPassword(cmPassword());
    }

    /** Check if the currently-configured password is known to have worked with the SSG. */
    public boolean passwordWorkedWithSsg() {
        return passwordWorkedWithSsg;
    }

    public void passwordWorkedWithSsg(boolean worked) {
        passwordWorkedWithSsg = worked;
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
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

    /**
     * Is this SSG marked as Default?
     * If a client request arrives via an endpoint not mapped to any SSG, it is routed to the Default SSG.
     * @return
     */
    public boolean isDefaultSsg() {
        return defaultSsg;
    }

    public void setDefaultSsg(boolean defaultSsg) {
        this.defaultSsg = defaultSsg;
    }

    /**
     * Get the pathname of the key store file this SSG will use.
     * @return the key store pathname
     */
    public synchronized String getKeyStorePath() {
        if (keyStorePath == null)
            keyStorePath = KEY_FILE + getId() + KEY_EXT;
        return keyStorePath;
    }

    /**
     * Set the pathname of the key store file this SSG should use.
     * @param keyStorePath the key store pathname
     */
    public void setKeyStorePath(String keyStorePath) {
        if (keyStorePath == null)
            throw new IllegalArgumentException("keyStorePath may not be null");
        if (keyStorePath.length() < 1)
            throw new IllegalArgumentException("keyStorePath may not be the empty string");
        this.keyStorePath = keyStorePath;
        flushKeyStoreData();
    }

    /**
     * Get the pathname of the trust store file this SSG will use.
     * @return the trust store pathname
     */
    public synchronized String getTrustStorePath() {
        if (trustStorePath == null)
            trustStorePath = TRUST_FILE + getId() + TRUST_EXT;
        return trustStorePath;
    }

    /**
     * Set the pathname of the trust store file this SSG will use
     * @param trustStorePath the trust store pathname
     */
    public void setTrustStorePath(String trustStorePath) {
        if (trustStorePath == null)
            throw new IllegalArgumentException("trustStorePath may not be null");
        if (trustStorePath.length() < 1)
            throw new IllegalArgumentException("trustStorePath may not be the empty string");
        this.trustStorePath = trustStorePath;
        flushKeyStoreData();
    }

    /** Flush any cached data from the key store. */
    private synchronized void flushKeyStoreData() {
        keyStore = null;
        trustStore = null;
        haveClientCert = null;
        privateKey = null;
        passwordWorkedForPrivateKey = false;
    }

    /** Key store file.  Package private; used by SsgKeyStoreManager. */
    File getKeyStoreFile() {
        return new File(getKeyStorePath());
    }

    /** Trust store file.  Package private; used by SsgKeyStoreManager. */
    File getTrustStoreFile() {
        return new File(getTrustStorePath());
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
     */
    Boolean haveClientCert() {
        return haveClientCert;
    }

    /**
     * Transient quick check of whether we have a client cert or not.
     * Package-private; used by SsgKeyStoreManager.
     */
    void haveClientCert(Boolean haveClientCert) {
        this.haveClientCert = haveClientCert;
    }

    /** Transient cache of private key for client cert in keystore; used by SsgKeyStoreManager. */
    PrivateKey privateKey() {
        return privateKey;
    }

    /** Transient cache of private key for client cert in keystore; used by SsgKeyStoreManager. */
    void privateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /** Transient check if this password worked to unlock the private key; used by SsgKeyStoreManager. */
    void passwordWorkedForPrivateKey(boolean worked) {
        this.passwordWorkedForPrivateKey = worked;
    }

    /** Transient check if this password worked to unlock the private key; used by SsgKeyStoreManager. */
    boolean passwordWorkedForPrivateKey() {
        return this.passwordWorkedForPrivateKey;
    }

    public synchronized int incrementNumTimesLogonDialogCanceled() {
        return numTimesLogonDialogCanceled++;
    }

    public void onCredentialsUpdated() {
        credentialsUpdatedTimeMillis = System.currentTimeMillis();
    }

    public long credentialsUpdatedTime() {
        return credentialsUpdatedTimeMillis;
    }

    public byte[] getPersistPassword() {
        return persistPassword;
    }

    public void setPersistPassword(byte[] persistPassword) {
        this.persistPassword = persistPassword;
    }

    /**
     * Get the configured credentials for this Ssg, if any.
     * @return The configuration credentials, or null if there aren't any yet.
     */
    public PasswordAuthentication getCredentials() {
        String username = getUsername();
        char[] password = cmPassword();
        if (username != null && getTrustedGateway() != null)
            return new PasswordAuthentication(username, new char[0]); // shield actual password in federated case
        if (username != null && username.length() > 0 && password != null)
            return new PasswordAuthentication(username, password);
        return null;
    }

    public boolean isChainCredentialsFromClient() {
        return chainCredentialsFromClient;
    }

    public void setChainCredentialsFromClient(boolean chainCredentialsFromClient) {
        this.chainCredentialsFromClient = chainCredentialsFromClient;
    }

    public boolean isUseSslByDefault() {
        return useSslByDefault;
    }

    public void setUseSslByDefault(boolean useSslByDefault) {
        this.useSslByDefault = useSslByDefault;
    }

    /** Get the trust manager used for SSL connections to this SSG. */
    public synchronized ClientProxyTrustManager trustManager() {
        if (trustManager == null) {
            trustManager = SslInstanceHolder.trustManager;
        }
        return trustManager;
    }

    /** Set the trust manager to use for SSL connections to this SSG. */
    public synchronized void trustManager(ClientProxyTrustManager tm) {
        if (tm == null)
            throw new IllegalArgumentException("TrustManager may not be null");
        trustManager = tm;
    }

    /**
     * Establish or reestablish the global SSL state.  Must be called after any change to client
     * or server certificates used during any SSL handshake, otherwise the implementation may cache
     * undesirable information.  (The cache is seperate from the session cache, too, so you can't just
     * flush the sessions to fix it.)
     */
    private synchronized SSLContext createSslContext()
    {
        log.info("Creating new SSL context for SSG " + toString());
        ClientProxyKeyManager keyManager = SslInstanceHolder.keyManager;
        ClientProxyTrustManager trustManager = trustManager();
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

    public synchronized SSLContext sslContext() {
        if (sslContext == null)
            sslContext = createSslContext();
        return sslContext;
    }

    /** Flush SSL context and cached certificates. */
    public synchronized void resetSslContext()
    {
        keyStore(null);
        trustStore(null);
        privateKey(null);
        passwordWorkedForPrivateKey(false);
        haveClientCert(null);
        clientCert(null);
        serverCert(null);
        clearSessionCookies();
        sslContext = createSslContext();
        serverCert = null;
        clientCert = null;
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public synchronized void serverCert(X509Certificate cert) {
        this.serverCert = cert;
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public synchronized void clientCert(X509Certificate cert) {
        this.clientCert = cert;
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public synchronized X509Certificate clientCert() {
        return clientCert;
    }

    /** Transient cached certificate for quicker access; only for use by SsgKeystoreManager. */
    public synchronized X509Certificate serverCert() {
        return serverCert;
    }

    /**
     * Return the HTTP cookies of the user session established with SiteMinder Policy Server.
     *
     * @return  Cookie[]  The list of session cookies.
     */
    public synchronized Cookie[] retrieveSessionCookies() {

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

    /**
     * Store the HTTP cookies of the user session established with SiteMinder Policy Server.
     *
     * @param cookies  The HTTP cookies to be saved.
     */
    public synchronized void storeSessionCookies(Cookie[] cookies) {
        sessionCookies = cookies;
    }

    /**
     * Delete the cookies.
     */
    public synchronized void clearSessionCookies() {
        sessionCookies = new Cookie[0];
    }

    /**
     * Transient record of SAML holder-of-key assertion for holder-of-key authentication.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public void samlHolderOfKeyAssertion(SamlHolderOfKeyAssertion ass) {
        samlHolderOfKeyAssertion = ass;
    }

    /**
     * Transient record of SAML holder-of-key assertion for holder-of-key authentication.
     * Don't use directly; go through PendingRequest to avoid races.
     */
    public SamlHolderOfKeyAssertion samlHolderOfKeyAssertion() {
        return samlHolderOfKeyAssertion;
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
    public long timeOffset() {
        return timeOffset;
    }

    /**
     * Set a time offset for this SSG.  See timeOffset() for details.
     * @param timeOffset the new offset to use, or 0 to disable clock translation.
     */
    public void timeOffset(long timeOffset) {
        this.timeOffset = timeOffset;
    }

    private DateTranslator fromSsgDateTranslator = new DateTranslator() {
        public Date translate(Date source) {
            if (source == null)
                return null;
            final long timeOffset = timeOffset();
            if (timeOffset == 0)
                return source;
            final Date result = new Date(source.getTime() - timeOffset);
            log.log(Level.FINE, "Translating date from SSG clock " + source + " to local clock " + result);
            return result;
        }
    };

    /**
     * Translate a date and time from the SSG's clock into our local clock.  Leaves the
     * date unchanged if a TimeOffset is not set for this SSG.  This is used to work around
     * clock-skew between the Bridge and this SSG; see timeOffset() for details.
     * @return a DateTranslator that will translate according to the Bridge's local clock setting
     */
    public DateTranslator dateTranslatorFromSsg() {
        return fromSsgDateTranslator;
    }

    private DateTranslator toSsgDateTranslator = new DateTranslator() {
        public Date translate(Date source) {
            if (source == null)
                return null;
            final long timeOffset = timeOffset();
            if (timeOffset == 0)
                return source;
            final Date result = new Date(source.getTime() + timeOffset);
            log.log(Level.FINE, "Translating date from local clock " + source + " to SSG clock " + result);
            return result;
        }
    };

    /**
     * Translate a date and time from the Bridge's local clock into the SSG's clock.  Leaves
     * the date unchanged if a TimeOffset is not set for this SSG.  This is used to work around
     * clock-skew between the Bridge and this SSG; see getTimeOFfset() for details.
     * @return a DateTranslator that will translate according to the SSG's clock setting
     */
    public DateTranslator dateTranslatorToSsg() {
        return toSsgDateTranslator;
    }

    public boolean isUseOverrideIpAddresses() {
        return useOverrideIpAddresses;
    }

    public void setUseOverrideIpAddresses(boolean useOverrideIpAddresses) {
        this.useOverrideIpAddresses = useOverrideIpAddresses;
    }

    public String[] getOverrideIpAddresses() {
        return overrideIpAddresses;
    }

    public void setOverrideIpAddresses(String[] overrideIpAddresses) {
        this.overrideIpAddresses = overrideIpAddresses;
    }

    public MultiThreadedHttpConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }
}
