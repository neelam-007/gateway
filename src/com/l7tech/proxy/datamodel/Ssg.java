package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.ClientProxy;
import org.apache.log4j.Category;

import java.io.Serializable;
import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.net.URL;
import java.net.MalformedURLException;
import java.security.KeyStore;

/**
 * In-core representation of an SSG.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:09:04 AM
 */
public class Ssg implements Serializable, Cloneable, Comparable {
    private static final Category log = Category.getInstance(Ssg.class);
    private static final String SSG_PROTOCOL = "http";
    private static final String SSG_FILE = "/ssg/servlet/soap";
    private static final int SSG_SSL_PORT = 8443;
    private static final int SSG_PORT = 8080;
    private static final String KEY_FILE = ClientProxy.PROXY_CONFIG + File.separator + "keyStore";
    private static final String CERT_REQUEST_FILE = "/ssg/csr";

    private long id = 0;
    private String name = "";
    private String localEndpoint = null;
    private String ssgAddress = "";
    private int ssgPort = SSG_PORT;
    private String ssgFile = SSG_FILE;
    private int sslPort = SSG_SSL_PORT;
    private String username = null;
    private transient char[] password = null;
    private boolean defaultSsg = false;

    private transient HashMap policyMap = new HashMap(); /* Policy cache */
    private transient boolean promptForUsernameAndPassword = true;
    private transient int numTimesLogonDialogCanceled = 0; /* Breaker to prevent spamming user with dialogs. */
    private transient KeyStore keyStore = null;
    private transient Boolean haveClientCert = null;

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
     * @param name      human-readable name of the Ssg.
     * @param serverAddress hostname or address of the associated SSG.
     */
    public Ssg(long id, final String name, final String serverAddress) {
        this(id);
        this.name = name;
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
        return getName() + (isDefaultSsg() ? " (Default)" : "");
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
    public synchronized void attachPolicy(String uri, String soapAction, Assertion policy)
            throws IllegalArgumentException
    {
        if (uri == null)
            uri = "";
        if (soapAction == null)
            soapAction = "";
        policyMap.put(new PolicyAttachmentKey(uri, soapAction), policy);
    }

    /**
     * Attach (or update) a policy for this SSG.  The policy will be filed
     * under the provided PolicyAttachmentKey.
     * @param key
     * @param policy
     */
    public synchronized void attachPolicy(PolicyAttachmentKey key, Assertion policy) {
        policyMap.put(key, policy);
    }

    /**
     * Look up a policy by PolicyAttachmentKey.
     * @param policyAttachmentKey the URI/SoapAction/etc to look up
     * @return the associated policy, or null if no such policy was found
     */
    public synchronized Assertion lookupPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return (Assertion)policyMap.get(policyAttachmentKey);
    }

    /**
     * Look up a policy by URI and SOAPAction.
     * @param uri The namespace of the first element within the SOAP message body.
     * @param soapAction the contents of the SOAPAction HTTP header.
     * @return A policy if found, or null
     */
    public synchronized Assertion lookupPolicy(String uri, String soapAction) {
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

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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
            url = new URL("https", getSsgAddress(), getSslPort(), CERT_REQUEST_FILE);
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

    public boolean isCredentialsConfigured() {
        return getUsername() != null && getPassword() != null && getUsername().length() > 0;
    }

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
    public String getSsgProtocol() {
        return SSG_PROTOCOL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(final char[] password) {
        this.password = password;
    }

    public void setPassword(final String password) {
        this.password = password.toCharArray();
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    /**
     * @deprecated this is only here because the bean serializer needs it. use lookupPolicy() instead
     * @return
     */
    public HashMap getPolicyMap() {
        return policyMap;
    }

    /**
     * @deprecated this is only here because the bean serialier needs it. use attachPolicy() instead
     * @param policyMap
     */
    public void setPolicyMap(HashMap policyMap) {
        if (policyMap == null || !(policyMap instanceof HashMap))
            throw new IllegalArgumentException("The policy map must be a valid HashMap");
        this.policyMap = policyMap;
    }

    public boolean isPromptForUsernameAndPassword() {
        return promptForUsernameAndPassword;
    }

    public void setPromptForUsernameAndPassword(boolean promptForUsernameAndPassword) {
        this.promptForUsernameAndPassword = promptForUsernameAndPassword;
    }

    public int getNumTimesLogonDialogCanceled() {
        return numTimesLogonDialogCanceled;
    }

    public void setNumTimesLogonDialogCanceled(int numTimesLogonDialogCanceled) {
        this.numTimesLogonDialogCanceled = numTimesLogonDialogCanceled;
    }

    public synchronized int incrementNumTimesLogonDialogCanceled() {
        return ++this.numTimesLogonDialogCanceled;
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

    /** Key store file.  Package private; used by ClientKeyManager. */
    File getKeyStoreFile() {
        return new File(KEY_FILE + getId());
    }

    /** Transient in-core cache of KeyStore.  Package private; used by ClientKeyManager. */
    KeyStore getKeyStore() {
        return keyStore;
    }

    /** Transient in-core cache of KeyStore.  Package private; used by ClientKeyManager. */
    void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Transient quick check of whether we have a client cert or not.
     * Package-private; used by ClientKeyManager.
     */
    Boolean getHaveClientCert() {
        return haveClientCert;
    }

    /**
     * Transient quick check of whether we have a client cert or not.
     * Package-private; used by ClientKeyManager.
     */
    void setHaveClientCert(Boolean haveClientCert) {
        this.haveClientCert = haveClientCert;
    }
}
