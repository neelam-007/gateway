package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import org.apache.log4j.Category;

/**
 * In-core representation of an SSG.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:09:04 AM
 */
public class Ssg implements Cloneable, Comparable {
    private static Category log = Category.getInstance(Ssg.class);

    private long id = 0;
    private String name = "";
    private String localEndpoint = "";
    private String serverUrl = "";
    private int sslPort = 443;
    private String username = null;
    private String password = null;
    private String keyStorePath = null;
    private Assertion policy = null;   // null = no policy set

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
        if (policy != null)
            ssg.setPolicy((Assertion)getPolicy().clone());
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

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
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

    public Assertion getPolicy() {
        return policy;
    }

    public void setPolicy(Assertion policy) {
        this.policy = policy;
    }
}
