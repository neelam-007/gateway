package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.*;

/**
 * Entity manager for SecurityZone.
 */
public interface SecurityZoneManager extends GoidEntityManager<SecurityZone, EntityHeader>, RoleAwareGoidEntityManager<SecurityZone> {
    /**
     * Create a read-only role for the given SecurityZone.
     *
     * @param zone the SecurityZone for which to create a read-only role.
     * @throws SaveException if unable to create the role.
     */
    void addReadSecurityZoneRole(SecurityZone zone) throws SaveException;

    /**
     * Create an manage (admin) role for the given SecurityZone.
     * @param zone the SecurityZone for which to create a manage (admin) role.
     * @throws SaveException if unable to create the role.
     */
    void addManageSecurityZoneRole(SecurityZone zone) throws SaveException;
}
