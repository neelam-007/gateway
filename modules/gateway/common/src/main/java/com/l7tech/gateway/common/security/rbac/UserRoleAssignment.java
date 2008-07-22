/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Statically assigns a {@link Role} to a {@link User}.
 * @author alex
 */
public class UserRoleAssignment extends PersistentEntityImp {
    protected long providerId;
    protected String userId;
    private Role role;

    public UserRoleAssignment(Role role, long providerId, String userId) {
        if (role == null) throw new NullPointerException();
        this.role = role;
        this.providerId = providerId;
        this.userId = userId;
    }

    protected UserRoleAssignment() { }

    public String getUserId() {
        return userId;
    }

    protected void setUserId(String userId) {
        this.userId = userId;
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UserRoleAssignment that = (UserRoleAssignment) o;

        if (providerId != that.providerId) return false;
        if (role != null ? !role.equals(that.role) : that.role != null) return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (providerId ^ (providerId >>> 32));
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }
}
