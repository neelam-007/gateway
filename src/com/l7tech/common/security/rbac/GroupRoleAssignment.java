/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.identity.Group;

/**
 * Statically assigns a {@link Role} to a {@link Group}.
 * @author alex
 */
public class GroupRoleAssignment extends IdentityRoleAssignment {
    public GroupRoleAssignment(Role role, long providerId, String identityId) {
        super(role, providerId, identityId);
    }

    protected GroupRoleAssignment() { }

    public String getGroupId() {
        return identityId;
    }

    protected void setGroupId(String groupId) {
        this.identityId = groupId;
    }
}
