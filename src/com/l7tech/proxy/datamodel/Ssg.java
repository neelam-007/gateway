package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;

/**
 * In-core representation of an SSG.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:09:04 AM
 * To change this template use Options | File Templates.
 */
public class Ssg implements Cloneable, Comparable {
    private static Category log = Category.getInstance(Ssg.class);

    private String name = "";
    private String localEndpoint = "";
    private String serverUrl = "";
    private String username = "";
    private String password = null;

    public int compareTo(final Object o) {
        final Ssg that = (Ssg)o;
        int res;

        // Check each field
        res = name.compareTo(that.name);
        if (res != 0)
            return res;

        res = localEndpoint.compareTo(that.localEndpoint);
        if (res != 0)
            return res;

        res = serverUrl.compareTo(that.serverUrl);
        if (res != 0)
            return res;

        res = username.compareTo(that.username);
        if (res != 0)
            return res;

        // none left
        return res;
    }

    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!this.getClass().equals(o.getClass()))
            return false;
        return hashCode() == o.hashCode();
    }

    public int hashCode() {
        return (int)(name.hashCode() * 257L +
                localEndpoint.hashCode() * 997L +
                serverUrl.hashCode() * 1103L +
                username.hashCode() * 37L);
    }

    /** Create a new Ssg instance with default field contents. */
    public Ssg() {
    }

    /**
     * Create a new Ssg instance with the given field contents.
     * @param name
     * @param localEndpoint
     * @param serverUrl
     * @param username
     * @param password
     */
    public Ssg(final String name, final String localEndpoint, final String serverUrl, final String username, final String password) {
        this.name = name;
        this.localEndpoint = localEndpoint;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * More user-friendly version of clone().  Caller is spared from having to catch
     * CloneNotSupportedException, or from having to cast the return value.
     */
    public Ssg getCopy() {
        try {
            return (Ssg)clone();
        } catch (CloneNotSupportedException e) {
            // this can't happen
            log.error(e);
            return null;
        }
    }

    public String toString() {
        return getName();
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
}
