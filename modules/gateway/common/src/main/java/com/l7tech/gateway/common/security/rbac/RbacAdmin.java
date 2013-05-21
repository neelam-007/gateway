/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionAccess;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;

/**
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Administrative
@Secured(types=EntityType.RBAC_ROLE)
public interface RbacAdmin {
    String ROLE_NAME_PREFIX = "Manage";
    String ROLE_NAME_PREFIX_READ = "View";
    String ROLE_NAME_OID_SUFFIX = " (#{1})";
    String RENAME_REGEX_PATTERN = "(?<=^\\w'{'1,128'}' ).*?(?= {0} \\(#\\d[\\d, ]*\\)$)";
    String ZONE_ROLE_NAME_TYPE_SUFFIX = "Zone";

    /**
     * Finds a collection of EntityHeaders for all {@link Role}s known to the SSG.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<Role> findAllRoles() throws FindException;

    /**
     * Returns the single Role with the specified OID, or null if no Role with the given OID exists.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITY)
    Role findRoleByPrimaryKey(long oid) throws FindException;

    /**
     * Gets the permissions of the current admin user.  This is unsecured, so that anyone running
     * the SSM can find out what functionality they can access. 
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false) 
    Collection<Permission> findCurrentUserPermissions() throws FindException;

    /**
     * Gets the roles assigned to the given user.  This is unsecured, so that anyone running
     * the SSM can find out what functionality they can access.
     * <p/>
     * <b>Note</b>: this method <em>intentionally</em> avoids validating user accounts (e.g. whether they're expired or disabled).
     * <b>Don't ever use it for authorization</b>!
     *
     * @param user the user whose roles to look up.  Must not be null.
     * @return the roles for this user.  May be empty but never null.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    Collection<Role> findRolesForUser(User user) throws FindException;

    /**
     * Saves the specified Role.
     * @return the OID of the role that was saved
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long saveRole(Role role) throws SaveException;

    @Secured(stereotype=DELETE_ENTITY)
    void deleteRole(Role selectedRole) throws DeleteException;

    @Transactional(readOnly=true)
    @Secured(types=EntityType.ANY, stereotype=FIND_HEADERS)
    EntityHeaderSet<EntityHeader> findEntities(EntityType entityType) throws FindException;

    @Transactional(readOnly=true)
    @Secured(types=EntityType.SECURITY_ZONE, stereotype=FIND_ENTITIES)
    Collection<SecurityZone> findAllSecurityZones() throws FindException;

    @Transactional(readOnly=true)
    @Secured(types=EntityType.SECURITY_ZONE, stereotype=FIND_ENTITY)
    SecurityZone findSecurityZoneByPrimaryKey(long oid) throws FindException;

    /**
     * Save a new or updated security zone.
     *
     * @param securityZone the zone to save.  Required.
     * @return the OID assigned to the zone.
     * @throws SaveException if the save fails.
     */
    @Secured(types=EntityType.SECURITY_ZONE, stereotype=SAVE_OR_UPDATE)
    long saveSecurityZone(SecurityZone securityZone) throws SaveException;

    @Secured(types=EntityType.SECURITY_ZONE, stereotype=DELETE_ENTITY)
    void deleteSecurityZone(SecurityZone securityZone) throws DeleteException;

    @Secured(stereotype = FIND_ENTITIES)
    Collection<Entity> findEntitiesByTypeAndSecurityZoneOid(@NotNull final EntityType type, final long securityZoneOid) throws FindException;

    /**
     * Obtain information about the list of assertions classnames that the current admin user is permitted to make use of.
     * Assertions whose classnames do not appear on this list should not be offered in the SSM palette
     * (and will very likely also not be permitted to be referenced by policy XML saved using the PolicyAdmin interface).
     *
     * @return a collection of permitted assertions.  Never null.
     * @throws FindException if there is a problem talking to the database.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.ASSERTION_ACCESS, stereotype=FIND_ENTITIES)
    Collection<AssertionAccess> findAccessibleAssertions() throws FindException;

    /**
     * Update assertion access information (eg, the security zone).
     * <p/>
     * This method will not be permitted to change the assertion classname of an existing AssertionAccess instance.
     *
     * @param assertionAccess the assertion access to update.
     * @return the up-to-date assertion access after saving.  Never null.
     * @throws UpdateException
     */
    @Secured(types=EntityType.ASSERTION_ACCESS, stereotype=UPDATE)
    long saveAssertionAccess(AssertionAccess assertionAccess) throws UpdateException;
}
