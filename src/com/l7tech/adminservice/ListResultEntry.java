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
        this.type = "";
    }

    public ListResultEntry(long uid, String name, String type) {
        this.uid = uid;
        this.name = name;
        this.type = type;
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

    /**
     * the type property is the class name of the object whose header this is
     * this is not a Class type because it needs to be jaxrpc serializable
     * @return the class name
     */
    public String getType() {
        if (type == null) type = "";
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private String name;
    private long uid;
    private String type;
}
