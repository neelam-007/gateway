/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import java.io.Serializable;

/**
 * @author alex
 * @version $Revision$
 */
public class AnonymousEntityReference implements NamedEntity, Serializable {
    public AnonymousEntityReference(Class entityClass, String uniqueId) {
        this.entityClass = entityClass;
        this.uniqueId = uniqueId;
        try {
            this.oid = Long.valueOf(uniqueId).longValue();
        } catch (NumberFormatException e) {
        }
    }

    public AnonymousEntityReference(Class entityClass, long oid) {
        this.entityClass = entityClass;
        this.oid = oid;
        this.uniqueId = Long.toString(oid);
    }

    public Class getEntityClass() {
        return entityClass;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public long getOid() {
        return oid;
    }

    public String getName() {
        return entityClass.getSimpleName() + " #" + oid;
    }

    public String toString() {
        return getName();
    }

    public int getVersion() {
        return 0;
    }

    /** @deprecated */
    public void setVersion(int version) {
        throw new UnsupportedOperationException();
    }

    /** @deprecated */
    public void setOid(long oid) {
        throw new UnsupportedOperationException();
    }

    /** @deprecated */
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 5741062976640065826L;

    private final Class entityClass;
    protected String uniqueId;
    private long oid;
}
