package com.l7tech.proxy.datamodel;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.Session;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.util.ClientLogger;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * In-core representation of an SSG.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:09:04 AM
 */
public class Ssg implements Serializable, Cloneable, Comparable {
    private static final ClientLogger log = ClientLogger.getInstance(Ssg.class);
    private static final String SSG_PROTOCOL = "http";
    private static final int SSG_SSL_PORT = 8443;
    private static final int SSG_PORT = 8080;
    private static final String KEY_FILE = ClientProxy.PROXY_CONFIG + File.separator + "keyStore";

    private long id = 0;
    private String localEndpoint = null;
    private String ssgAddress = "";
    private int ssgPort = SSG_PORT;
    private String ssgFile = SecureSpanConstants.SSG_FILE;
    private int sslPort = SSG_SSL_PORT;
    private String username = null;
    private boolean defaultSsg = false;
    private boolean savePasswordToDisk = false;
    private byte[] persistPassword = null;

    // These fields are transient.  To prevent the bean serializer from saving them anyway,
    // they do not use the getFoo() / setFoo() naming convention in their accessors and mutators.
    private transient char[] password = null;
    private transient HashMap policyMap = new HashMap(); /* Policy cache */
    private transient boolean promptForUsernameAndPassword = true;
    private transient KeyStore keyStore = null;
    private transient Boolean haveClientCert = null;
    private transient int numTimesLogonDialogCanceled = 0;
    private transient long credentialsUpdatedTimeMillis = 0;
    private transient Session session = null;
    private transient List listeners = new ArrayList(); // List of weak references to listeners
    private transient PrivateKey privateKey = null; // cache of private key
    private transient boolean passwordWorkedForPrivateKey = false;
    private transient boolean passwordWorkedWithSsg = false;

    public int compareTo(final Object o) {
        long id0 = getId();
        long id1 = ((Ssg)o).getId();
        if (id0 == 0 || id1 == 0)
            throw new IllegalArgumentException("Comparison of Ssgs without Ids is not defined");
        return id0 < id1 ? -1 : id0 > id1 ? 1 : 0;
    }

    public boolean equals(final Object o) {
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
    }

    /** Create a new Ssg instance with the given ID. */
    public Ssg(long id) {
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

    public Object clone() throws CloneNotSupportedException {
        Ssg ssg = (Ssg)super.clone();
        ssg.policyMap = (HashMap)this.policyMap.clone();
        return ssg;
    }

    /**
     * More user-friendly version of clone().  Caller is spared from having to catch
     * CloneNotSupportedException, or from having to cast the return value.
     * @param newId the Id to use for the new Ssg instance
     */
    public Ssg getCopy(long newId) {
        try {
            Ssg clone = (Ssg)clone();
            clone.setId(0);
            return clone;
        } catch (CloneNotSupportedException e) {
            // this can't happen
            log.error(e);
            return null;
        }
    }

    /**
     * More user-friendly version of clone().  Caller is spared from having to catch
     * CloneNotSupportedException, or from having to cast the return value.
     * The new Ssg will not have a valid Id.
     */
    public Ssg getCopy() {
        return getCopy(0);
    }

    public String toString() {
        return getSsgAddress() + (isDefaultSsg() ? " (Default)" : "");
    }

    /**
     * Add a listener to be notified of changes to this Ssg's state, including policy changes.
     * Adding a listener requires that a Gui be available, since event delivery is done on the Swing
     * thread.
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
                        else
                            throw new IllegalArgumentException("Unknown event type: " + event.getType());
                    }
                }
            }
        });
    }

    /** Fire a new POLICY_ATTACHED event. */
    private void firePolicyAttachedEvent(PolicyAttachmentKey pak, Policy policy) {
        fireSsgEvent(SsgEvent.createPolicyAttachedEvent(this, pak, policy));
    }

    /**
     * Attach (or update) a policy for this SSG.  The policy will be filed
     * under the specified URI and/or SOAPAction, at least one of which must
     * be provided.
     * @param uri
     * @param soapAction
     * @param policy
     * @throws IllegalArgumentException if neither uri nor soapAction is specified, or policy is null
     */
    public synchronized void attachPolicy(String uri, String soapAction, Policy policy )
            throws IllegalArgumentException
    {
        if (uri == null)
            uri = "";
        if (soapAction == null)
            soapAction = "";
        PolicyAttachmentKey key = new PolicyAttachmentKey( uri, soapAction );
        attachPolicy( key, policy );
    }

    /**
     * Attach (or update) a policy for this SSG.  The policy will be filed
     * under the provided PolicyAttachmentKey.
     * @param key
     * @param policy
     */
    public synchronized void attachPolicy(PolicyAttachmentKey key, Policy policy ) {
        policyMap.put(key, policy);
        firePolicyAttachedEvent(key, policy);
    }

    /**
     * Look up a policy by PolicyAttachmentKey.
     * @param policyAttachmentKey the URI/SoapAction/etc to look up
     * @return the associated policy, or null if no such policy was found
     */
    public synchronized Policy lookupPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return (Policy)policyMap.get(policyAttachmentKey);
    }

    /**
     * Look up a policy by URI and SOAPAction.
     * @param uri The namespace of the first element within the SOAP message body.
     * @param soapAction the contents of the SOAPAction HTTP header.
     * @return A policy if found, or null
     */
    public synchronized Policy lookupPolicy(String uri, String soapAction) {
        return lookupPolicy(new PolicyAttachmentKey(uri, soapAction));
    }

    /**
     * Get the set of PolicyAttachmentKey that we currently know about.
     * These can then be passed to lookupPolicy() to get the policies.
     * @return a defensively-copied Set of PolicyAttachmentKey objects.
     */
    public synchronized Set getPolicyAttachmentKeys() {
        Set setCopy = new TreeSet(policyMap.keySet());
        return setCopy;
    }

    /**
     * Clear all cached policies.
     */
    public synchronized void clearPolicies() {
        policyMap.clear();
    }

    /* generated getters and setters */

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLocalEndpoint() {
        if (localEndpoint == null)
            localEndpoint = "ssg" + getId();
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
            log.error(e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                log.error("This can't have happened", e);
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
            log.error(e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                log.error("This can't have happened", e);
                return null; // totally can't happen
            }
        }
        return url;
    }

    public URL getServerCertRequestUrl() {
        URL url = null;
        try {
            url = new URL("https", getSsgAddress(), getSslPort(), SecureSpanConstants.CERT_REQUEST_FILE);
        } catch (MalformedURLException e) {
            log.error(e);
            try {
                return new URL("");
            } catch (MalformedURLException e1) {
                log.error("This can't have happened", e1);
                return null;
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
    }

    /** Obfuscate the password for storage to disk in plaintext. */
    private byte[] obfuscatePassword(char[] password) {
        if (password == null)
            return null;
        BASE64Encoder be = new BASE64Encoder();
        String obfuscated = be.encode(new String(password).getBytes());
        return obfuscated.getBytes();
    }

    /** De-obfuscate a password that was stored to disk in plaintext. */
    private char[] deobfuscatePassword(byte[] persistPassword) {
        if (persistPassword == null)
            return null;
        BASE64Decoder bd = new BASE64Decoder();
        try {
            return new String(bd.decodeBuffer(new String(persistPassword))).toCharArray();
        } catch (IOException e) {
            log.error("Unable to recover persisted password", e);
            return null;
        }
    }

    /**
     * Read the in-memory cached password for this SSG. Should be used only by a CredentialManager
     * or the SsgPropertiesDialog.
     * Others should use the CredentialManager's getCredentials() method instead.
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

    /** Cached session.  Package private; used by SsgSessionManager. */
    Session session() {
        return session;
    }

    /** Set cached session.  Package private; used by SsgSessionManager. */
    void session(Session session) {
        this.session = session;
    }

    /** Key store file.  Package private; used by SsgKeyStoreManager. */
    File getKeyStoreFile() {
        return new File(KEY_FILE + getId());
    }

    /** Transient in-core cache of KeyStore.  Package private; used by SsgKeyStoreManager. */
    KeyStore keyStore() {
        return keyStore;
    }

    /** Transient in-core cache of KeyStore.  Package private; used by SsgKeyStoreManager. */
    void keyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
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
        if (username != null && username.length() > 0 && password != null)
            return new PasswordAuthentication(username, password);
        return null;
    }
}
