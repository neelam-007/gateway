/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * @author alex
 */
public interface RbacAdmin {

    /**
     * Finds a collection of EntityHeaders for all {@link Role}s known to the SSG.
     */
    Collection<Role> findAllRoles() throws FindException, RemoteException;

    /**
     * Returns the single Role with the specified OID, or null if no Role with the given OID exists.
     */
    Role findRoleByPrimaryKey(long oid) throws FindException, RemoteException;

    /**
     * The collection of Roles granted to the specified User.
     * @return the collection of Roles granted to the specified User.  Never null, may be empty.
     * @param includeGroups <code>true</code> to include roles granted to groups of which the specified User is a member, <code>false</code> otherwise.
     */
    Collection<Role> findRolesForUser(User user, boolean includeGroups) throws FindException, RemoteException;

    /**
     * Saves the specified Role.
     * @return the OID of the role that was saved
     */
    long saveRole(Role role) throws SaveException, RemoteException;

    /**
     * Grants the specified Role to the specified User.
     */
    void addUserToRole(User user, Role role) throws UpdateException, RemoteException;

    /**
     * Drops the specified User from the specified Role.
     */
    void removeUserFromRole(User user, Role role) throws UpdateException, RemoteException;

    void deleteRole(Role selectedRole) throws DeleteException, RemoteException;

    Collection<User> findAssignedUsers(Role role) throws FindException, RemoteException;

    EntityHeader[] findEntities(Class<? extends Entity> entityClass) throws FindException, RemoteException;
}
