package com.l7tech.adminservice;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 6, 2003
 *
 * Convenient type to return in the admin Web Service for list queries
 */
public class ListResultEntry {
    public ListResultEntry() {
        this.uid = 0;
        this.name = "";
    }
    public ListResultEntry(long uid, String name) {
        this.uid = uid;
        this.name = name;
    }
    public long getUid() {
        return uid;
    }
    public void setUid(long uid) {
        this.uid = uid;
    }
    public String getName() {
        if (name == null) name = "";
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private String name;
    private long uid;
}
