package com.l7tech.adminws.service;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 */
public class Header {
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
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
    // ************************************************
    // PRIVATES
    // ************************************************
    private long oid;
    private String type;
    private String name;
}
