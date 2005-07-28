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
public class DeletedEntity implements NamedEntity, Serializable {
    public DeletedEntity(Class entityClass, String uniqueId) {
        this.entityClass = entityClass;
        this.uniqueId = uniqueId;
        try {
            this.oid = Long.valueOf(uniqueId).longValue();
        } catch (NumberFormatException e) {
        }
    }

    public DeletedEntity(Class entityClass, long oid) {
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
        return "<deleted " + entityClass.getName() + ">";
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

    public long getLoadTime() {
        return 0;
    }

    /** @deprecated */
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    private final Class entityClass;
    private String uniqueId;
    private long oid;
}
