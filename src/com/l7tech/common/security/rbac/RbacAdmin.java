/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
@Secured(types=EntityType.RBAC_ROLE)
public interface RbacAdmin {

    /**
     * Finds a collection of EntityHeaders for all {@link Role}s known to the SSG.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<Role> findAllRoles() throws FindException, RemoteException;

    /**
     * Returns the single Role with the specified OID, or null if no Role with the given OID exists.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_BY_PRIMARY_KEY)
    Role findRoleByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * Gets the permissions of the current admin user.  This is unsecured, so that anyone running
     * the SSM can find out what functionality they can access. 
     */
    @Transactional(readOnly=true)
    Collection<Permission> findCurrentUserPermissions() throws FindException, RemoteException;

    /**
     * Saves the specified Role.
     * @return the OID of the role that was saved
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long saveRole(Role role) throws SaveException, RemoteException;

    @Secured(stereotype=DELETE_ENTITY)
    void deleteRole(Role selectedRole) throws DeleteException, RemoteException;

    @Transactional(readOnly=true)
    @Secured(types=EntityType.ANY, stereotype=FIND_HEADERS)
    EntityHeader[] findEntities(Class<? extends Entity> entityClass) throws FindException, RemoteException;
}
