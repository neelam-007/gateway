package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;

import java.util.HashMap;
import java.util.Map;

import com.l7tech.policy.assertion.Assertion;

/**
 * In-core representation of an SSG.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:09:04 AM
 */
public class Ssg implements Cloneable, Comparable {
    private static final Category log = Category.getInstance(Ssg.class);

    private long id = 0;
    private String name = "";
    private String localEndpoint = "";
    private String serverUrl = "";
    private int sslPort = 443;
    private String username = null;
    private char[] password = null;
    private String keyStorePath = null;
    private HashMap policiesByUri = new HashMap();
    private HashMap policiesBySoapAction = new HashMap();
    private boolean promptForUsernameAndPassword = true;

    private transient int numTimesLogonDialogCanceled = 0;

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
     * @param name
     * @param localEndpoint
     * @param serverUrl
     */
    public Ssg(long id, final String name, final String localEndpoint, final String serverUrl) {
        this(id);
        this.name = name;
        this.localEndpoint = localEndpoint;
        this.serverUrl = serverUrl;
    }

    public Object clone() throws CloneNotSupportedException {
        Ssg ssg = (Ssg)super.clone();
        ssg.policiesByUri = (HashMap)this.policiesByUri.clone();
        ssg.policiesBySoapAction = (HashMap)this.policiesBySoapAction.clone();
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
        return getName();
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
        boolean haveUri = uri != null && uri.length() > 0;
        boolean haveSoapAction = soapAction != null && soapAction.length() > 0;
        if (!haveUri && !haveSoapAction)
            throw new IllegalArgumentException("Must specify either uri or soapAction");
        if (policy == null)
            throw new IllegalArgumentException("No policy was specified");
        if (haveUri)
            policiesByUri.put(uri, policy);
        if (haveSoapAction)
            policiesBySoapAction.put(soapAction, policy);
    }

    /**
     * Look up a policy by URI.
     * @param uri The namespace of the first element within the SOAP message body.
     * @return A policy if found, or null
     */
    public synchronized Assertion getPolicyByUri(String uri) {
        return (Assertion)policiesByUri.get(uri);
    }

    /**
     * Look up a policy by SoapAction.
     * @param soapAction The operation (minus fragment, if any) specified in the SOAPAction: header.
     * @return A policy if found, or null
     */
    public synchronized Assertion getPolicyBySoapAction(String soapAction) {
        return (Assertion)policiesBySoapAction.get(soapAction);
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
        return localEndpoint;
    }

    public void setLocalEndpoint(final String localEndpoint) {
        this.localEndpoint = localEndpoint;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(final String serverUrl) {
        this.serverUrl = serverUrl;
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

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(final String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    /**
     * Directly obtain our lookup map.
     * This is here for javax.beans.XMLWriter to use; please do not call this directly.
     * @return
     */
    public HashMap getPoliciesByUri() {
        return policiesByUri;
    }

    /**
     * Directly update our lookup map.
     * This is here for javax.beans.XMLWriter to use; please do not call this directly.
     */
    public void setPoliciesByUri(HashMap policiesByUri) {
        this.policiesByUri = policiesByUri;
    }

    /**
     * Directly obtain our lookup map.
     * This is here for javax.beans.XMLWriter to use; please do not call this directly.
     * @return
     */
    public HashMap getPoliciesBySoapAction() {
        return policiesBySoapAction;
    }

    /**
     * Directly update our lookup map.
     * This is here for javax.beans.XMLWriter to use; please do not call this directly.
     */
    public void setPoliciesBySoapAction(HashMap policiesBySoapAction) {
        this.policiesBySoapAction = policiesBySoapAction;
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
}
