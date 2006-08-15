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
    public AnonymousEntityReference(Class entityClass, long oid) {
        this(entityClass, oid, null);
    }

    public AnonymousEntityReference(Class entityClass, String uniqueId) {
        this(entityClass, uniqueId, null);
    }

    public AnonymousEntityReference(Class entityClass, String uniqueId, String name) {
        this.entityClass = entityClass;
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public AnonymousEntityReference(Class entityClass, long oid, String name) {
        this.entityClass = entityClass;
        this.name = name;
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

    public String getId() {
        return uniqueId;
    }

    public String getName() {
        if (name != null) return name;
        return entityClass.getSimpleName() + " #" + uniqueId;
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
    private final String name;
    protected String uniqueId;
}
