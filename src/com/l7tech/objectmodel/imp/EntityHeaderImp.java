package com.l7tech.objectmodel.imp;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 */
public class EntityHeaderImp implements com.l7tech.objectmodel.EntityHeader {
    public EntityHeaderImp( long oid, Class type, String name ) {
        this.oid = oid;
        this.type = type;
        this.name = name;
    }

    public EntityHeaderImp() {
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
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
    long oid;
    String name;
    Class type;
}
