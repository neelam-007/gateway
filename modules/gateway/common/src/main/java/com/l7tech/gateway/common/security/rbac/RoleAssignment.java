/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Statically assigns a {@link Role} to a {@link User}.
 * @author alex
 *
 * EntityType is enforced in constructor but for hibernate initial implementation the value is stored as a string
 */
public class RoleAssignment extends PersistentEntityImp {
    protected long providerId;
    protected String identityId;
    private Role role;

    private String entityType;

    public RoleAssignment(Role role, long providerId, String identityId, EntityType entityType) {
        if (role == null) throw new NullPointerException();
        this.role = role;
        this.providerId = providerId;
        this.identityId = identityId;
        this.entityType = entityType.getName();
    }

    protected RoleAssignment() { }

    public String getIdentityId() {
        return identityId;
    }

    protected void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public long getProviderId() {
        return providerId;
    }

    protected void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public Role getRole() {
        return role;
    }

    protected void setRole(Role role) {
        this.role = role;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String type) {
        this.entityType = type;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RoleAssignment that = (RoleAssignment) o;

        if (providerId != that.providerId) return false;
        if (role != null ? !role.equals(that.role) : that.role != null) return false;
        if (identityId != null ? !identityId.equals(that.identityId) : that.identityId != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (providerId ^ (providerId >>> 32));
        result = 31 * result + (identityId != null ? identityId.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }
}
