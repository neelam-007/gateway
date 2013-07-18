/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.objectmodel.EntityType;

import javax.persistence.Table;
import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.UniqueConstraint;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Proxy;

/**
 * Statically assigns a {@link Role} to a {@link User}.
 * @author alex
 *
 * EntityType is enforced in constructor but for hibernate initial implementation the value is stored as a string
 */
@Entity
@Proxy(lazy=false)
@Table(name="rbac_assignment",
       uniqueConstraints=@UniqueConstraint(columnNames={"provider_oid", "role_oid", "identity_id", "entity_type"})
)
public class RoleAssignment extends PersistentEntityImp {
    protected long providerId;
    protected String identityId;
    private Role role;

    private String entityType;

    public RoleAssignment(Role role, long providerId, String identityId, EntityType entityType) {
        if (role == null) throw new IllegalArgumentException("Role cannot be null");
        this.role = role;
        this.providerId = providerId;
        this.identityId = identityId;
        this.entityType = entityType.getName();
    }

    protected RoleAssignment() { }

    @Column(name="identity_id", nullable=false, length=255)
    public String getIdentityId() {
        return identityId;
    }

    protected void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    @Column(name="provider_oid", nullable=false)
    public long getProviderId() {
        return providerId;
    }

    protected void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="role_oid", nullable=false)
    public Role getRole() {
        return role;
    }

    protected void setRole(Role role) {
        this.role = role;
    }

    @Column(name="entity_type", nullable=false, length=50)
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String type) {
        this.entityType = type;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RoleAssignment that = (RoleAssignment) o;

        if (providerId != that.providerId) return false;
        if (role != null ? (that.role == null || role.getOid() != that.role.getOid()) : that.role != null) return false;
        if (identityId != null ? !identityId.equals(that.identityId) : that.identityId != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (providerId ^ (providerId >>> 32));
        result = 31 * result + (identityId != null ? identityId.hashCode() : 0);
        result = 31 * result + (role != null ? (int)(role.getOid() ^ (role.getOid() >>> 32)) : 0);
        return result;
    }
}
