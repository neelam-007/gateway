package com.l7tech.proxy.datamodel;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.util.HexUtils;
import com.l7tech.proxy.ClientProxy;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ssg settings that get loaded from and saved to ssgs.xml.  Does not contain any behaviour; for that,
 * see {@link SsgRuntime}.
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
    private PersistentPolicyManager persistentPolicyManager = new PersistentPolicyManager(); // policy store that gets saved to disk

    private Set listeners = new HashSet(); // List of weak references to listeners
    private SsgRuntime runtime = new SsgRuntime(this);

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
        getRuntime().flushKeyStoreData();
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
        getRuntime().flushKeyStoreData();
    }

    /** Key store file.  Package private; used by SsgKeyStoreManager. */
    File getKeyStoreFile() {
        return new File(getKeyStorePath());
    }

    /** Trust store file.  Package private; used by SsgKeyStoreManager. */
    File getTrustStoreFile() {
        return new File(getTrustStorePath());
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

    public boolean isUseSslByDefault() {
        return useSslByDefault;
    }

    public void setUseSslByDefault(boolean useSslByDefault) {
        this.useSslByDefault = useSslByDefault;
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
            throw new IllegalArgumentException("Event to be fired must identify this Ssg as the source");
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
}
