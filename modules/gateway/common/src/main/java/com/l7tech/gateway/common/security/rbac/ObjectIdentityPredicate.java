/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Matches any {@link com.l7tech.objectmodel.Entity} that is of the expected type and whose
 * {@link com.l7tech.objectmodel.Entity#getId()} matches {@link #targetEntityId}.
 */
@javax.persistence.Entity
@Proxy(lazy=false)
@Table(name="rbac_predicate_oid")
public class ObjectIdentityPredicate extends ScopePredicate implements ScopeEvaluator {
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

    @Override
    public ScopePredicate createAnonymousClone() {
        ObjectIdentityPredicate copy = new ObjectIdentityPredicate(null, this.targetEntityId);
        copy.setGoid(this.getGoid());
        copy.header = this.header;
        return copy;
    }

    @Column(name="entity_id", nullable=false, length=255)
    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(String targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public boolean matches(Entity entity) {
        EntityType etype = permission.getEntityType();
        if (etype == null || etype == EntityType.ANY)
            throw new IllegalStateException("Can't evaluate an ObjectIdentityPredicate without a specific EntityType");
        // Type has already been checked by {@link Permission#matches}
        // The hex of the goid should all be in lowercase but use equalsIgnoreCase incase it is not for some reason sanity
        return targetEntityId.equalsIgnoreCase(entity.getId());
    }

    @Transient
    public EntityHeader getHeader() {
        return header;
    }

    public void setHeader(EntityHeader header) {
        this.header = header;
    }

    public String toString() {
        if (header != null)
            return getName(header) + " " + header.getType().getName();
        
        StringBuilder sb = new StringBuilder(permission == null ? "" : permission.getEntityType().getName());
        sb.append(" #").append(targetEntityId);
        return sb.toString();
    }

    private static String getName(EntityHeader header) {
        return header.getName() != null ? header.getName() : header.getStrId();
    }

    @SuppressWarnings({"RedundantIfStatement"})
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
