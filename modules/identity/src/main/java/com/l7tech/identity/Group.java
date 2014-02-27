package com.l7tech.identity;

import com.l7tech.security.rbac.RbacAttribute;

/**
 * Represents a set of {@link User}s.
 *
 * Memberships are queried and managed using the {@link com.l7tech.gateway.common.admin.IdentityAdmin} API rather than through properties of
 * the Group objects themselves.
 */
public interface Group extends Identity {
    /**
     * Gets a human-readable description for this Group
     * @return a human-readable description for this Group
     */
    @RbacAttribute
    String getDescription();

    /**
     * @return a human-readable name for this Group.
     */
    @RbacAttribute
    String getName();
}
