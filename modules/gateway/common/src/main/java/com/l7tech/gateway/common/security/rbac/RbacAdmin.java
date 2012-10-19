/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
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
     *
     * The User cannot be null.
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
}
