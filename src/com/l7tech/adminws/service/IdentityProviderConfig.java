package com.l7tech.adminws.service;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 */
public class IdentityProviderConfig {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public long getTypeOid() {
        return typeOid;
    }

    public void setTypeOid(long typeOid) {
        this.typeOid = typeOid;
    }

    public String getTypeClassName() {
        return typeClassName;
    }

    public void setTypeClassName(String typeClassName) {
        this.typeClassName = typeClassName;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private String name;
    private String description;
    private long oid;
    private String typeName;
    private String typeDescription;
    private long typeOid;
    private String typeClassName;
}
