package com.l7tech.server.identity;

import com.l7tech.objectmodel.Goid;

/**
 * An identity provider that has a default role.
 * <p/>
 * If an identity provider declares a default role, then any successfully-authenticated user from
 * that provider that does not have any explicit role assignments will be considered by RbacServices
 * to be in the declared default role.
 */
public interface HasDefaultRole {
    /**
     * @return the default role for this identity provider, or null if not configured.
     */
    Goid getDefaultRoleId();
}
