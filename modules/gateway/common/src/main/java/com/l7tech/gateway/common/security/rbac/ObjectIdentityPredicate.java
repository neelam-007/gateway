/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;

/**
 * Matches any {@link com.l7tech.objectmodel.Entity} that is of the expected type and whose
 * {@link com.l7tech.objectmodel.Entity#getId()} matches {@link #targetEntityId}.
 */
public class ObjectIdentityPredicate extends ScopePredicate {
    private String targetEntityId;
    private EntityHeader header;

    public ObjectIdentityPredicate(Permission permission, String targetEntityId) {
        super(permission);
        this.targetEntityId = targetEntityId;
    }

    public ObjectIdentityPredicate(Permission permission, long targetEntityOid) {
        super(permission);
        this.targetEntityId = Long.toString(targetEntityOid);
    }

    protected ObjectIdentityPredicate() { }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(String targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    boolean matches(Entity entity, Class<? extends Entity> eclass) {
        EntityType etype = permission.getEntityType();
        if (etype == null || etype == EntityType.ANY)
            throw new IllegalStateException("Can't evaluate an ObjectIdentityPredicate without a specific EntityType");
        // Type has already been checked by {@link Permission#matches}
        return targetEntityId.equals(entity.getId());
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
        sb.append(" #").append(targetEntityId);
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ObjectIdentityPredicate that = (ObjectIdentityPredicate) o;

        if (targetEntityId != null ? !targetEntityId.equals(that.targetEntityId) : that.targetEntityId != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (targetEntityId != null ? targetEntityId.hashCode() : 0);
        return result;
    }
}
