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
    public AnonymousEntityReference(Class<? extends Entity> entityClass, String uniqueId) {
        this(entityClass, uniqueId, null);
    }

    public AnonymousEntityReference(Class<? extends Entity> entityClass, String uniqueId, String name) {
        this.entityClass = entityClass;
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public AnonymousEntityReference(Class<? extends Entity> entityClass, Goid oid, String name) {
        this.entityClass = entityClass;
        this.name = name;
        this.uniqueId = Goid.toString(oid);
    }

    public Class<? extends Entity> getEntityClass() {
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
    @Deprecated
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 5741062976640065826L;

    private final Class<? extends Entity> entityClass;
    private final String name;
    protected String uniqueId;
}
