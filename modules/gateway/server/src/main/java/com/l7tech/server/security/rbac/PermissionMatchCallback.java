/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.Permission;

/**
 * Used by {@link RoleManager#deleteEntitySpecificRole(PermissionMatchCallback)} to decide whether a given Role can be deleted
 */
public interface PermissionMatchCallback {
    /**
     * @return true if the supplied Permission meets the deletion criteria
     */
    boolean matches(Permission permission);
}
