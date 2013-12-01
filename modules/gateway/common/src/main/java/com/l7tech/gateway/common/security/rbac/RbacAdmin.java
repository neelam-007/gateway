/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionAccess;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

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
    String RENAME_REGEX_PATTERN = "(?<=^\\w'{'1,128'}' ).*?(?= {0} \\(#[\\d[\\d, ]|[A-Za-z0-9]]*\\)$)";
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
    Role findRoleByPrimaryKey(Goid oid) throws FindException;

    /**
     * Gets the permissions of the current admin user.  This is unsecured, so that anyone running
     * the SSM can find out what functionality they can access. 
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    Collection<Permission> findCurrentUserPermissions() throws FindException;

    /**
     * Gets the roles assigned to the given user.  This is unsecured, so that anyone running
     * the SSM can find out what functionality they can access.
     * <p/>
     * <b>Note</b>: this method <em>intentionally</em> avoids validating user accounts (e.g. whether they're expired or disabled).
     * <b>Don't ever use it for authorization</b>!
     * <p/>
     * The returned collection will not include the default role, if any -- only explicitly assigned role assignments.
     * This means this method may return an empty collection for a user that would obtain a non-empty result
     * from {@link #findCurrentUserPermissions()}.
     * <p/>
     * To check for a default role, use the {@link #findDefaultRoleForIdentityProvider} method with the user's
     * identity provider ID.
     *
     * @param user the user whose roles to look up.  Must not be null.
     * @return the roles for this user.  May be empty but never null.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    Collection<Role> findRolesForUser(User user) throws FindException;

    /**
     * Gets the default role for an identity provider, if it has one.
     * <p/>
     * This method is here to provide a way to access this information for RBAC GUI purposes
     * regardless of what permissions the current admin may have (or not have) on the identity provider.
     *
     * @param identityProviderId ID of identity provider to check for a default role.  Required.
     * @return the default role of the specified identity provider, if there is one, or null.
     * @throws FindException on error obtaining information.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    Role findDefaultRoleForIdentityProvider(Goid identityProviderId) throws FindException;

    /**
    * Gets the roles assigned to the given group.
    *
    * This method does <em>not</em> check the validity of the group and should not be used for authorization.
    * @param group the group for which to look up roles it has been assigned to.
    * @return a collection of roles to which the group is assigned.
    * @throws FindException
    */
    @NotNull
    @Transactional(readOnly = true)
    @Secured(stereotype = FIND_ENTITIES)
    Collection<Role> findRolesForGroup(@NotNull final Group group) throws FindException;
    /**
     * Saves the specified Role.
     * @return the OID of the role that was saved
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid saveRole(Role role) throws SaveException;

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
    SecurityZone findSecurityZoneByPrimaryKey(Goid goid) throws FindException;

    /**
     * Save a new or updated security zone.
     *
     * @param securityZone the zone to save.  Required.
     * @return the GOID assigned to the zone.
     * @throws SaveException if the save fails.
     */
    @Secured(types=EntityType.SECURITY_ZONE, stereotype=SAVE_OR_UPDATE)
    Goid saveSecurityZone(SecurityZone securityZone) throws SaveException;

    @Secured(types=EntityType.SECURITY_ZONE, stereotype=DELETE_ENTITY)
    void deleteSecurityZone(SecurityZone securityZone) throws DeleteException;

    /**
     * Retrieves a collection of ZoneableEntityHeader by type and security zone goid.
     *
     * @param type             the EntityType to retrieve.
     * @param securityZoneGoid the goid of the SecurityZone that the entities must be in.
     * @return a collection of EntityHeader of the given EntityType which are in a SecurityZone identified by the given goid.
     * @throws FindException            if a db error occurs.
     * @throws IllegalArgumentException if the given EntityType is not security zoneable.
     */
    @Secured(stereotype = FIND_ENTITIES)
    Collection<EntityHeader> findEntitiesByTypeAndSecurityZoneGoid(@NotNull final EntityType type, final Goid securityZoneGoid) throws FindException;

    /**
     * Sets the given SecurityZone on a collection of entities.
     *
     * @param securityZoneGoid the goid of the SecurityZone to set on the entities or null to remove the entities from their current SecurityZone.
     * @param entityType       the EntityType of the entities. Must be a zoneable EntityType.
     * @param entityIds        a collection of object ids that identify the entities to update.
     * @throws UpdateException if a db error occurs or any of the object ids provided do not identify existing entities.
     */
    @Secured(customInterceptor = "com.l7tech.server.security.rbac.SecurityZoneRbacInterceptor")
    public void setSecurityZoneForEntities(final Goid securityZoneGoid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws UpdateException;

    /**
     * Sets the given SecurityZone on a map of entities.
     *
     * @param securityZoneGoid the goid of the SecurityZone to set on the entities or null to remove the entities from their current SecurityZone.
     * @param entityIds        a map where key = entity type of the entities to update and value = collection of oids which represent the entities to update.
     * @throws UpdateException if a db error occurs or any of the object ids provided do not identify existing entities.
     */
    @Secured(customInterceptor = "com.l7tech.server.security.rbac.SecurityZoneRbacInterceptor")
    public void setSecurityZoneForEntities(final Goid securityZoneGoid, @NotNull Map<EntityType, Collection<Serializable>> entityIds) throws UpdateException;

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
    @Secured(types=EntityType.ASSERTION_ACCESS, stereotype=SAVE_OR_UPDATE)
    Goid saveAssertionAccess(AssertionAccess assertionAccess) throws UpdateException;
}
