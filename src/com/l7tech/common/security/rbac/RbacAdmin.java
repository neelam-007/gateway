/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * @author alex
 */
public interface RbacAdmin {

    /**
     * Finds a collection of EntityHeaders for all {@link Role}s known to the SSG.
     */
    Collection<EntityHeader> findAll() throws FindException, RemoteException;

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
     * Returns the collection of Roles granted to the specified Group.  Never null, may be empty.
     */
    Collection<Role> findRolesForGroup(Group user) throws FindException, RemoteException;

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

    /**
     * Grants the specified Role to members of the specified Group.
     */
    void addGroupToRole(Group group, Role role) throws UpdateException, RemoteException;

    /**
     * Drops the specified Group from the specified Role.
     */
    void removeGroupFromRole(Group group, Role role) throws UpdateException, RemoteException;
}
