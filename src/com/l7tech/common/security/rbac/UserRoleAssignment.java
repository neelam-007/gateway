/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.identity.User;

/**
 * Statically assigns a {@link Role} to a {@link User}.
 * @author alex
 */
public class UserRoleAssignment extends IdentityRoleAssignment {
    public UserRoleAssignment(Role role, long providerId, String userId) {
        super(role, providerId, userId);
    }

    protected UserRoleAssignment() { }

    public String getUserId() {
        return identityId;
    }

    protected void setUserId(String userId) {
        this.identityId = userId;
    }
}
