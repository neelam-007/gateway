package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.xmlenc.Session;
import org.apache.log4j.Category;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

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
    private boolean defaultSsg = false;

    // These fields are transient.  To prevent the bean serializer from saving them anyway,
    // they do not use the getFoo() / setFoo() naming convention in their accessors and mutators.
    private transient char[] password = null;
    private transient HashMap policyMap = new HashMap(); /* Policy cache */
    private transient HashMap clientPolicyMap = new HashMap(); /* Client policy cache */
    private transient boolean promptForUsernameAndPassword = true;
    private transient KeyStore keyStore = null;
    private transient Boolean haveClientCert = null;
    private transient int numTimesLogonDialogCanceled = 0;
    private transient long credentialsUpdatedTimeMillis = 0;
    private transient Session session = null;

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
        ssg.clientPolicyMap = (HashMap)this.clientPolicyMap.clone();
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
    public synchronized void attachPolicy(String uri, String soapAction, Assertion policy )
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
    public synchronized void attachPolicy(PolicyAttachmentKey key, Assertion policy ) {
        policyMap.put(key, policy);
        ClientAssertion clientPolicy = ClientPolicyFactory.getInstance().makeClientPolicy( policy );
        clientPolicyMap.put( key, clientPolicy );
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
     * Look up a clientPolicy by PolicyAttachmentKey.
     * @param policyAttachmentKey the URI/SoapAction/etc to look up
     * @return the associated policy, or null if no such policy was found
     */
    public synchronized ClientAssertion lookupClientPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return (ClientAssertion)clientPolicyMap.get(policyAttachmentKey);
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
     * Look up a clientPolicy by URI and SOAPAction.
     * @param uri The namespace of the first element within the SOAP message body.
     * @param soapAction the contents of the SOAPAction HTTP header.
     * @return A policy if found, or null
     */
    public synchronized ClientAssertion lookupClientPolicy(String uri, String soapAction) {
        return lookupClientPolicy( new PolicyAttachmentKey(uri, soapAction) );
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
        clientPolicyMap.clear();
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

    /**
     * Check if a non-null and non-empty username, and a non-null password, are configured for this SSG.
     * @return
     */
    public boolean isCredentialsConfigured() {
        return getUsername() != null && password() != null && getUsername().length() > 0;
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

    public char[] password() {
        return password;
    }

    public void password(final char[] password) {
        this.password = password;
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

    public synchronized int incrementNumTimesLogonDialogCanceled() {
        return numTimesLogonDialogCanceled++;
    }

    public void onCredentialsUpdated() {
        credentialsUpdatedTimeMillis = System.currentTimeMillis();
    }

    public long credentialsUpdatedTime() {
        return credentialsUpdatedTimeMillis;
    }
}
