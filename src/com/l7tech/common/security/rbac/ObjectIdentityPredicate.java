/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;

/**
 * Matches any {@link Entity} that is of the expected type and has a matching {@link Entity#getOid()}.
 */
public class ObjectIdentityPredicate extends ScopePredicate {
    private Entity targetEntity;

    public ObjectIdentityPredicate(Permission permission, Entity targetEntity) {
        super(permission);
        this.targetEntity = targetEntity;
    }

    protected ObjectIdentityPredicate() { }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(Entity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public boolean matches(Entity entity) {
        EntityType etype = permission.getEntityType();
        if (etype == null || etype == EntityType.ANY)
            throw new IllegalStateException("Can't evaluate an ObjectIdentityPredicate without a specific EntityType");
        return etype.getEntityClass().isAssignableFrom(entity.getClass()) && targetEntity.getOid() == entity.getOid();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ObjectIdentityPredicate that = (ObjectIdentityPredicate) o;

        if (targetEntity != null ? !targetEntity.equals(that.targetEntity) : that.targetEntity != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (targetEntity != null ? targetEntity.hashCode() : 0);
        return result;
    }
}
