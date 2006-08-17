package com.l7tech.proxy.datamodel;

import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.proxy.ssl.SslPeer;

import javax.net.ssl.SSLContext;
import javax.swing.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.PasswordAuthentication;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ssg settings that get loaded from and saved to ssgs.xml.  Does not contain any behaviour; for that,
 * see {@link SsgRuntime}.
 */
public class Ssg implements Serializable, Cloneable, Comparable, SslPeer {
    private static final Logger log = Logger.getLogger(Ssg.class.getName());
    private static final String SSG_PROTOCOL = "http";
    private static final int SSG_SSL_PORT = 8443;
    private static final int SSG_PORT = 8080;

    public static final String PROXY_CONFIG =
            System.getProperties().getProperty("user.home") + File.separator + ".l7tech";
    // Threshold for storing attachments on disk instead of in ram
    public static final int ATTACHMENT_DISK_THRESHOLD = 131071;
    public static final File ATTACHMENT_DIR = new File(PROXY_CONFIG + "/attachments");

    // The file that contains our client cert private key for this Ssg
    private static final String KEY_FILE = PROXY_CONFIG + File.separator + "key";
    private static final String KEY_EXT = ".p12";

    // The file that contains the trusted server cert for this Ssg
    private static final String TRUST_FILE = PROXY_CONFIG + File.separator + "certs";
    private static final String TRUST_EXT = ".p12";

    private long id = 0;
    private Ssg trustedGateway = null;
    private String localEndpoint;
    private String ssgAddress = "";
    private int ssgPort = SSG_PORT;
    private String ssgFile = SecureSpanConstants.SSG_FILE;
    private int sslPort = SSG_SSL_PORT;
    private String username = null;
    private File keyStoreFile = null;
    private File trustStoreFile = null;
    private boolean defaultSsg = false;
    private boolean chainCredentialsFromClient = false;
    private String kerberosName = null;
    private boolean enableKerberosCredentials = false;
    private boolean useSslByDefault = true;
    private boolean httpHeaderPassthrough = false;
    private boolean savePasswordToDisk = false;
    private byte[] persistPassword = null;
    private boolean useOverrideIpAddresses = false;
    private String[] overrideIpAddresses = null;
    private PersistentPolicyManager persistentPolicyManager = new PersistentPolicyManager(); // policy store that gets saved to disk
    private FederatedSamlTokenStrategy fedSamlTokenStrategy = null; // non-default saml token strategy, or null
    private String serverUrl = null; // Special URL for generic (non-SSG) services

    private transient Set listeners = Collections.synchronizedSet(new HashSet()); // Set of weak references to listeners
    private transient SsgRuntime runtime = new SsgRuntime(this);
    private transient X509Certificate lastSeenPeerCertificate = null;
    private String failoverStrategyName = FailoverStrategyFactory.ORDERED.getName();
    private boolean genericService = false; // true if the server is not actually an SSG

    private static Set headersToCopy = new TreeSet(String.CASE_INSENSITIVE_ORDER);
    static {
        headersToCopy.add("Cooke");
        headersToCopy.add("Set-Cookie");
    }

    /**
     * Copy all persistent (non-Runtime) settings from that Ssg into this Ssg (except Id), overwriting this Ssg's
     * previous state.  Afterward, this Ssg's Runtime, listeners, and other transient state will be reset.
     * Caller is responsibile for ensuring that any necessary external state is kept in sync (ie, keystore files
     * needed by the Runtime's keystore manager, and which need to have pass phrases matching the persistent
     * password, if any).
     *
     * @param that   the Ssg from which we will copy our settings.
     */
    public void copyFrom(Ssg that) {
        try {
            BeanInfo stuff = Introspector.getBeanInfo(Ssg.class);
            PropertyDescriptor[] props = stuff.getPropertyDescriptors();
            for (int i = 0; i < props.length; i++) {
                PropertyDescriptor prop = props[i];
                if ("id".equalsIgnoreCase(prop.getName())) continue; // don't copy the Id
                if ("defaultssg".equalsIgnoreCase(prop.getName())) continue; // don't change the default SSG
                Method getter = prop.getReadMethod();
                Method setter = prop.getWriteMethod();
                if (getter != null && setter != null) {
                    try {
                        Object value = getter.invoke(that, new Object[0]);
                        setter.invoke(this, new Object[] { value });
                    } catch (IllegalAccessException e) {
                        // Ignore this and continue
                        log.log(Level.FINE, "Warning: unable to copy Ssg property: " + prop.getName() + ": " + ExceptionUtils.getMessage(e), e);
                    } catch (InvocationTargetException e) {
                        // Ignore this and continue
                        log.log(Level.FINE, "Warning: unable to copy Ssg property: " + prop.getName() + ": " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e); // can't happen
        }

        // Reset local endpoint
        this.localEndpoint = null;

        // Reset transients (except listeners)
        this.runtime = new SsgRuntime(this);
        this.lastSeenPeerCertificate = null;
    }

    /**
     * Get the {@link SsgRuntime} for this Ssg, providing access to behaviour, strategies, and transient settings.
     *
     * @return the SsgRuntime for this Ssg.  Never null.
     */
    public SsgRuntime getRuntime() {
        return runtime;
    }

    /** Create a new Ssg instance with default fields. */
    public Ssg() {
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

    public String toString() {
        return getSsgAddress();
    }

    /**
     * Get the PolicyManager whose cached policies are saved to disk.  Should be used only by GUI code
     * and the bean serializer.
     *
     * @return the persistent policy manager.  Never null.
     */
    public PersistentPolicyManager getPersistentPolicyManager() {
        return persistentPolicyManager;
    }

    /** Needed for bean serializer only; do not use. */
    public void setPersistentPolicyManager(PersistentPolicyManager persistentPolicyManager) {
        if (persistentPolicyManager == null) persistentPolicyManager = new PersistentPolicyManager(); // just in case
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
     * <p>
     * Even if this method returns null, this Ssg might still represent a Federated Gateway -- it just means
     * that it does not use a Trusted Gateway as a token provider.  To check if this Ssg represents a Federated Gateway,
     * use {@link #isFederatedGateway()}.
     *
     * @return the Trusted Gateway associated with this Federated Gateway, or null if not applicable.
     */
    public Ssg getTrustedGateway() {
        return trustedGateway;
    }

    public void setTrustedGateway(Ssg trustedGateway) {
        this.trustedGateway = trustedGateway;
    }

    public boolean isFederatedGateway() {
        return isFederatedTrusted() || getWsTrustSamlTokenStrategy() != null;
    }

    /** @return true if this is a federated gateway hanging off a trusted gateway. */
    public boolean isFederatedTrusted() {
       return trustedGateway != null;
    }

    /** @return true if this is a federated gateway that uses a WS-Trust STS. */
    public boolean isFederatedWsTrust() {
        return getWsTrustSamlTokenStrategy() instanceof WsTrustSamlTokenStrategy;
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

    public URL computeSsgUrl() {
        URL url = null;
        try {
            url = new URL(SSG_PROTOCOL, getSsgAddress(), getSsgPort(), getSsgFile());
        } catch (MalformedURLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                throw new RuntimeException(e1); // can't happen
            }
        }
        return url;
    }

    public String getServerUrl() {
        if (serverUrl == null && !isGeneric()) serverUrl = computeSsgUrl().toExternalForm();
        return serverUrl;
    }

    /**
     * Set the server URL.
     * If the urlStr is not null, this also sets the SSG hostname,
     * port (either SSL or regular depending if the URL is http or https),
     * and file.
     */
    public void setServerUrl(String urlStr) throws MalformedURLException {
        this.serverUrl = urlStr;
        if (urlStr == null)
            return;
        if (urlStr.equals(computeSsgUrl().toExternalForm())) // If it's the same as the one we'd generate, no need to change anything else.
            return;

        URL url = new URL(urlStr);
        setSsgAddress(url.getHost());
        if ("https".equalsIgnoreCase(url.getProtocol()))
            setSslPort(url.getPort());
        else
            setSsgPort(url.getPort());
        setSsgFile(url.getFile());
    }

    public URL computeServerSslUrl() throws MalformedURLException {
        URL url = new URL(getServerUrl());
        if (isGeneric() || "https".equals(url.getProtocol())) return url;
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
        getRuntime().clearSessionCookies();

        fireDataChangedEvent();
    }

    public boolean isSavePasswordToDisk() {
        return savePasswordToDisk;
    }

    public void setSavePasswordToDisk(boolean savePasswordToDisk) {
        this.savePasswordToDisk = savePasswordToDisk;
        getRuntime().setCachedPassword(getRuntime().getCachedPassword());
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    /**
     * Is this SSG marked as Default?
     * If a client request arrives via an endpoint not mapped to any SSG, it is routed to the Default SSG.
     * @return true if this is the default Ssg
     */
    public boolean isDefaultSsg() {
        return defaultSsg;
    }

    public void setDefaultSsg(boolean defaultSsg) {
        this.defaultSsg = defaultSsg;
    }

    /**
     * Set the pathname of the key store file this SSG should use.
     * @param keyStorePath the key store pathname
     * @deprecated this is only here for backward compat with old config files.  Use {@link #setKeyStoreFile} instead.
     */
    public void setKeyStorePath(String keyStorePath) {
        if (keyStorePath == null)
            throw new IllegalArgumentException("keyStorePath may not be null");
        if (keyStorePath.length() < 1)
            throw new IllegalArgumentException("keyStorePath may not be the empty string");
        setKeyStoreFile(new File(keyStorePath));
        getRuntime().flushKeyStoreData();
    }

    /**
     * Set the file to use as the key store for this SSG.
     *
     * @param file the key store file.  Must not be null.
     */
    public void setKeyStoreFile(File file) {
        if (file == null) throw new NullPointerException();
        this.keyStoreFile = file;
        getRuntime().flushKeyStoreData();
    }

    /**
     * Set the pathname of the trust store file this SSG will use
     * @param trustStorePath the trust store pathname
     * @deprecated this is here only for backward compatibility.  Use {@link #setTrustStoreFile} instead.
     */
    public void setTrustStorePath(String trustStorePath) {
        if (trustStorePath == null)
            throw new IllegalArgumentException("trustStorePath may not be null");
        if (trustStorePath.length() < 1)
            throw new IllegalArgumentException("trustStorePath may not be the empty string");
        setTrustStoreFile(new File(trustStorePath));
    }

    /**
     * Set the trust store file this SSG will use.
     *
     * @param file the trust store file.  Must not be null.
     */
    public void setTrustStoreFile(File file) {
        if (file == null) throw new IllegalArgumentException("trustStoreFile may not be null");
        this.trustStoreFile = file;
        getRuntime().flushKeyStoreData();
    }

    /**
     * Get the File of the key store this SSG will use.
     *
     * @return the key store pathname.  Never null.
     */
    public File getKeyStoreFile() {
        if (keyStoreFile == null)
            keyStoreFile = new File(KEY_FILE + getId() + KEY_EXT);
        return keyStoreFile;
    }

    /**
     * Get the trust store file.
     *
     * @return the trust store file.  Never null.
     */
    public File getTrustStoreFile() {
        if (trustStoreFile == null)
            trustStoreFile = new File(TRUST_FILE + getId() + TRUST_EXT);
        return trustStoreFile;
    }

    public byte[] getPersistPassword() {
        return persistPassword;
    }

    public void setPersistPassword(byte[] persistPassword) {
        this.persistPassword = persistPassword;
    }

    public boolean isChainCredentialsFromClient() {
        return chainCredentialsFromClient;
    }

    public void setChainCredentialsFromClient(boolean chainCredentialsFromClient) {
        this.chainCredentialsFromClient = chainCredentialsFromClient;
    }

    public String getKerberosName() {
        return kerberosName;
    }

    public void setKerberosName(String kerberosName) {
        this.kerberosName = kerberosName;
    }

    public boolean isEnableKerberosCredentials() {
        return this.enableKerberosCredentials;
    }

    public void setEnableKerberosCredentials(boolean enableKerberosCredentials) {
        this.enableKerberosCredentials = enableKerberosCredentials;
    }

    public boolean isUseSslByDefault() {
        return useSslByDefault;
    }

    public void setUseSslByDefault(boolean useSslByDefault) {
        this.useSslByDefault = useSslByDefault;
    }

    public boolean isHttpHeaderPassthrough() {
        return httpHeaderPassthrough;
    }

    public void setHttpHeaderPassthrough(boolean httpHeaderPassthrough) {
        this.httpHeaderPassthrough = httpHeaderPassthrough;
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

    /**
     * Non-default SAML token strategy, if any.
     *
     * NOTE: this should really be named getFederatedSamlTokenStrategy but due to
     * configuration file compatiblity the name is left as is.
     */
    public FederatedSamlTokenStrategy getWsTrustSamlTokenStrategy() {
        return fedSamlTokenStrategy;
    }

    /**
     * Non-default SAML token strategy, if any, or null to initialize with the default strategy.
     *
     * NOTE: this should really be named setFederatedSamlTokenStrategy but due to
     * configuration file compatiblity the name is left as is.
     */
    public void setWsTrustSamlTokenStrategy(FederatedSamlTokenStrategy newSamlTokenStrategy) {
        this.fedSamlTokenStrategy = newSamlTokenStrategy;
    }

    /** Obfuscate the password for storage to disk in plaintext. */
    public static byte[] obfuscatePassword(char[] password) {
        if (password == null)
            return null;

        String obfuscated = HexUtils.encodeBase64(new String(password).getBytes());
        return obfuscated.getBytes();
    }

    /** De-obfuscate a password that was stored to disk in plaintext. */
    public static char[] deobfuscatePassword(byte[] persistPassword) {
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
     * Add a listener to be notified of changes to this Ssg's state, including policy changes.
     * Adding a listener requires that a Gui be available, since event delivery is done on the Swing
     * thread.  The listeners are stored in a Set; thus you may add the same listener instance multiple
     * times, but it will only receive one copy of each event.  Must be called on Swing thread.
     */
    public void addSsgListener(SsgListener listener) {
        listeners.add(new WeakReference(listener));
    }

    /**
     * Remove a listener from the queue.  Must be called on Swing thread.
     */
    public void removeSsgListener(SsgListener listener) {
        synchronized (listeners) {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                WeakReference reference = (WeakReference) i.next();
                Object o = reference.get();
                if (o == null || o == listener)
                    i.remove();
            }
        }
    }

    /**
     * Notify all listeners that an SsgEvent has occurred.  Does not need to be called on Swing thread.
     *
     * @param event  the event to transmit
     */
    private void fireSsgEvent(final SsgEvent event) {
        if (event.getSource() != this)
            throw new IllegalArgumentException("Event to be fired must identify this Ssg as the source");
        if (listeners.size() < 1)
            return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (listeners) {
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
            }
        });
    }

    /** Fire a new DATA_CHANGED event. */
    private void fireDataChangedEvent() {
        fireSsgEvent(SsgEvent.createDataChangedEvent(this));
    }

    /** @return This Ssg's server cert, or null if it is not currently known or configured. */
    public X509Certificate getServerCertificate() {
        for (;;) {
            try {
                return getRuntime().getSsgKeyStoreManager().getServerCert();
            } catch (KeyStoreCorruptException e) {
                log.log(Level.WARNING, "Unable to read server certificate for Ssg " + this + ": " + e.getMessage(), e);
                try {
                    getRuntime().handleKeyStoreCorrupt();
                } catch (OperationCanceledException e1) {
                    throw new RuntimeException(e1); // cancel
                }
                /* FALLTHROUGH and retry */
            }
        }
    }

    /**
     * Get the server cert, or throw an exception which will eventually fault it in.
     *
     * @return this Ssg's server certificate.  Never null.
     * @throws ServerCertificateUntrustedException if the cert is not currently configured.  If this is thrown,
     *         {@link com.l7tech.proxy.ssl.CurrentSslPeer#get()} will be set to return this Ssg instance.
     */
    public X509Certificate getServerCertificateAlways() throws ServerCertificateUntrustedException {
        X509Certificate cert = getServerCertificate();
        if (cert == null) {
            CurrentSslPeer.set(this);
            throw new ServerCertificateUntrustedException("Server certificate for " + this + " needed, but not yet configured");
        }
        return cert;
    }

    public X509Certificate getClientCertificate() {
        for (;;) {
            try {
                return getRuntime().getSsgKeyStoreManager().getClientCert();
            } catch (KeyStoreCorruptException e) {
                log.log(Level.WARNING, "Unable to read client certificate for Ssg " + this + ": " + e.getMessage(), e);
                try {
                    getRuntime().handleKeyStoreCorrupt();
                } catch (OperationCanceledException e1) {
                    throw new RuntimeException(e1); // cancel it
                }
                /* FALLTHROUGH and retry */
            }
        }
    }

    /**
     * Get the private key.  Might take a long time if it needs to prompt for a password.
     * @return the private key, or null if we don't yet have a client certificate.
     */
    public PrivateKey getClientCertificatePrivateKey() throws BadCredentialsException {
        return getClientCertificatePrivateKey(null);
    }

    /**
     * Get the private key.  Might take a long time if it needs to prompt for a password.
     * @param passwordAuthentication OPTIONAL credentials to use (may be null)
     * @return the private key, or null if we don't yet have a client certificate.
     */
    public PrivateKey getClientCertificatePrivateKey(PasswordAuthentication passwordAuthentication) throws BadCredentialsException {
        PasswordAuthentication pa = passwordAuthentication;
        for (;;) {
            try {
                return getRuntime().getSsgKeyStoreManager().getClientCertPrivateKey(pa);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to read private key from keystore", e);
            } catch (KeyStoreCorruptException e) {
                final String msg = "Unable to read client certificate for Ssg " + this + ": " + e.getMessage();
                log.log(Level.WARNING, msg, e);
                try {
                    getRuntime().handleKeyStoreCorrupt();
                } catch (OperationCanceledException e1) {
                    throw new RuntimeException(msg, e1); // cancel it
                }
                /* FALLTHROUGH and retry */
            } catch (OperationCanceledException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getHostname() {
        return getSsgAddress();
    }

    public void storeLastSeenPeerCertificate(X509Certificate actualPeerCert) {
        this.lastSeenPeerCertificate = actualPeerCert;
    }

    public X509Certificate getLastSeenPeerCertificate() {
        return lastSeenPeerCertificate;
    }

    public SSLContext getSslContext() {
        return getRuntime().getSslContext();
    }

    public String getFailoverStrategyName() {
        return failoverStrategyName;
    }

    public void setFailoverStrategyName(String name) {
        failoverStrategyName = name;
    }

    /** @param generic  true if this service is not actually an SSG, but is just a generic URL to be posted to. */
    public void setGeneric(boolean generic) {
        this.genericService = generic;
    }

    /** @return true if this service is not actually an SSG, but is just a generic URL to be posted to. */
    public boolean isGeneric() {
        return this.genericService;
    }

    /** @return true if the specified HTTP header should be copied through. */
    public boolean shouldCopyHeader(String headerName) {
        return headersToCopy.contains(headerName);
    }
}
