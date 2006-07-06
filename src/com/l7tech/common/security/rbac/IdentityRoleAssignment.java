/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.identity.Identity;
import com.l7tech.objectmodel.imp.EntityImp;

/**
 * A static assignment of a {@link Role} to an {@link Identity}.
 */
public abstract class IdentityRoleAssignment extends EntityImp {
    protected long providerId;
    protected String identityId;
    private Role role;

    protected IdentityRoleAssignment() { }

    protected IdentityRoleAssignment(Role role, long providerId, String identityId) {
        this.role = role;
        this.providerId = providerId;
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
}
