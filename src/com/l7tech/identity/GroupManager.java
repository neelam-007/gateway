package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.Set;

/**
 * @author alex
 */
public interface GroupManager extends EntityManager {
    Group findByPrimaryKey( String oid ) throws FindException;
    Group findByName( String name ) throws FindException;
    void delete( Group group ) throws DeleteException;
    long save( Group group ) throws SaveException;
    void update( Group group ) throws UpdateException;

    EntityHeader groupToHeader( Group group );
    Group headerToGroup( EntityHeader header ) throws FindException;

    /**
     * Add a set of Users to a single Group.  If the set is null or empty, nothing is done.
     * @param group
     * @param users
     * @throws FindException
     * @throws UpdateException
     */
    void addUsers( Group group, Set users ) throws FindException, UpdateException;

    /**
     * Remove a set of Users from a single Group.  If the set is null or empty, nothing is done.
     * @param group
     * @param users
     * @throws FindException
     * @throws UpdateException
     */
    void removeUsers( Group group, Set users ) throws FindException, UpdateException;

    /**
     * Add a single User to a Set of Groups.  If the set is null or empty, nothing will be done.
     * @param user
     * @param groups
     * @throws FindException
     * @throws UpdateException
     */
    void addUser( User user, Set groups ) throws FindException, UpdateException;

    /**
     * Remove a single User from a Set of Groups.  If the set is null or empty, nothing will be done.
     * @param user
     * @param groups
     * @throws FindException
     * @throws UpdateException
     */
    void removeUser( User user, Set groups ) throws FindException, UpdateException;

    /**
     * Add a single User to a single Group.
     * @param user
     * @param group
     * @throws FindException
     * @throws UpdateException
     */
    void addUser( User user, Group group ) throws FindException, UpdateException;

    /**
     * Remove a single User from a single Group.
     * @param user
     * @param group
     * @throws FindException
     * @throws UpdateException
     */
    void removeUser( User user, Group group ) throws FindException, UpdateException;

    /**
     * Retrieve the Set of Groups to which a particular User belongs.
     * @param user
     * @return
     * @throws FindException
     */
    Set getGroups( User user ) throws FindException;

    /**
     * Retrieve the Set of Users belonging to a particular Group.
     * @param group
     * @return
     * @throws FindException
     */
    Set getUsers( Group group ) throws FindException;
}
