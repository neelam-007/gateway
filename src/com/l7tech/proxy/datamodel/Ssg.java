package com.l7tech.proxy.datamodel;

import org.apache.log4j.Category;

import java.text.Collator;

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

    public int compareTo(Object o) {
        Ssg that = (Ssg)o;
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

    public boolean equals(Object o) {
        return compareTo(o) == 0;
    }

    public int hashCode() {
        return name.hashCode() + localEndpoint.hashCode() + serverUrl.hashCode() + username.hashCode();
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
    public Ssg(String name, String localEndpoint, String serverUrl, String username, String password) {
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

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalEndpoint() {
        return localEndpoint;
    }

    public void setLocalEndpoint(String localEndpoint) {
        this.localEndpoint = localEndpoint;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
