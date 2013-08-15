/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.GoidEntityImp;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;

import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

/**
 * Statically assigns a {@link Role} to a {@link User}.
 * @author alex
 *
 * EntityType is enforced in constructor but for hibernate initial implementation the value is stored as a string
 */
@Entity
@Proxy(lazy=false)
@Table(name="rbac_assignment",
       uniqueConstraints=@UniqueConstraint(columnNames={"provider_goid", "role_goid", "identity_id", "entity_type"})
)
public class RoleAssignment extends GoidEntityImp {
    protected Goid providerId;
    protected String identityId;
    private Role role;

    private String entityType;
    private boolean inherited;

    public RoleAssignment(Role role, Goid providerId, String identityId, EntityType entityType) {
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

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    @Column(name="provider_goid", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getProviderId() {
        return providerId;
    }

    protected void setProviderId(Goid providerId) {
        this.providerId = providerId;
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="role_goid", nullable=false)
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

    /**
     * @return true if this role assignment is applicable due to group membership, not because the identity is directly assigned to this role.
     */
    @Transient
    public boolean isInherited() {
        return inherited;
    }

    /**
     * @param inherited set to true if this role assignment is applicable due to group membership, not because the identity is directly assigned to this role.
     */
    public void setInherited(final boolean inherited) {
        this.inherited = inherited;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RoleAssignment that = (RoleAssignment) o;

        if (identityId != null ? !identityId.equals(that.identityId) : that.identityId != null) return false;
        if (providerId != null ? !providerId.equals(that.providerId) : that.providerId != null) return false;
        if ((role == null) != (that.role == null)) return false;
        if (role != null && !Goid.equals(role.getGoid(), that.role.getGoid())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
        result = 31 * result + (identityId != null ? identityId.hashCode() : 0);
        result = 31 * result + (role != null && role.getGoid() != null ? role.getGoid().hashCode() : 0);
        return result;
    }
}
