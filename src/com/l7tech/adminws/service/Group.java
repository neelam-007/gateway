package com.l7tech.adminws.service;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 */
public class Group {
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getOid() {
        return oid;
    }
    public void setOid(long oid) {
        this.oid = oid;
    }
    public Header[] getMembers() {
        return members;
    }
    public void setMembers(Header[] members) {
        this.members = members;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private String description;
    private String name;
    private long oid;
    private Header[] members;
}
