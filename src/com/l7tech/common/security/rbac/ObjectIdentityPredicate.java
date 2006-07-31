/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;

import java.util.Set;

/**
 * Matches any {@link Entity} that is of the expected type and has a matching {@link Entity#getOid()}.
 */
public class ObjectIdentityPredicate extends ScopePredicate {
    private long targetEntityOid;
    private EntityHeader header;

    public ObjectIdentityPredicate(Permission permission, long targetEntityOid) {
        super(permission);
        this.targetEntityOid = targetEntityOid;
    }

    protected ObjectIdentityPredicate() { }

    public long getTargetEntityOid() {
        return targetEntityOid;
    }

    public void setTargetEntityOid(long targetEntityOid) {
        this.targetEntityOid = targetEntityOid;
    }

    public boolean matches(Entity entity) {
        EntityType etype = permission.getEntityType();
        if (etype == null || etype == EntityType.ANY)
            throw new IllegalStateException("Can't evaluate an ObjectIdentityPredicate without a specific EntityType");
        return etype.getEntityClass().isAssignableFrom(entity.getClass()) && targetEntityOid == entity.getOid();
    }

    public EntityHeader getHeader() {
        return header;
    }

    public void setHeader(EntityHeader header) {
        this.header = header;
    }

    public String toString() {
        if (header != null)
            return header.toString();
        
        StringBuilder sb = new StringBuilder(permission.getEntityType().getName());
        sb.append(" #").append(targetEntityOid);
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ObjectIdentityPredicate that = (ObjectIdentityPredicate) o;

        if (targetEntityOid != that.targetEntityOid) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (targetEntityOid ^ (targetEntityOid >>> 32));
        return result;
    }
}
