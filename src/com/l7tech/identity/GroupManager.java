package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Set;

/**
 * @author alex
 */
public interface GroupManager {
    Group findByPrimaryKey( String identifier ) throws FindException;
    Group findByName( String name ) throws FindException;
    void delete( Group group ) throws DeleteException, ObjectNotFoundException;
    void delete( String identifier ) throws DeleteException, ObjectNotFoundException;
    void deleteAll( long ipoid ) throws DeleteException, ObjectNotFoundException;
    void deleteAllVirtual( long ipoid ) throws DeleteException, ObjectNotFoundException;
    String save( Group group ) throws SaveException;
    void update( Group group ) throws UpdateException, ObjectNotFoundException;
    String save( Group group, Set<IdentityHeader> userHeaders ) throws SaveException;
    void update( Group group, Set<IdentityHeader> userHeaders ) throws UpdateException, ObjectNotFoundException;
    Collection search(String searchString) throws FindException;
    Class getImpClass();

    /**
     * Test whether a given User is a member of the specified Group.
     * @param user
     * @param group
     * @return
     * @throws FindException
     */
    boolean isMember( User user, Group group ) throws FindException;

    /**
     * Add a setInstance of Users to a single Group.  If the set is null or empty, nothing is done.
     * @param group
     * @param users
     * @throws FindException
     * @throws UpdateException
     */
    void addUsers( Group group, Set<User> users ) throws FindException, UpdateException;

    /**
     * Remove a setInstance of Users from a single Group.  If the set is null or empty, nothing is done.
     * @param group
     * @param users
     * @throws FindException
     * @throws UpdateException
     */
    void removeUsers( Group group, Set<User> users ) throws FindException, UpdateException;

    /**
     * Add a single User to a Set of Groups.  If the set is null or empty, nothing will be done.
     * @param user
     * @param groups
     * @throws FindException
     * @throws UpdateException
     */
    void addUser( User user, Set<Group> groups ) throws FindException, UpdateException;

    /**
     * Remove a single User from a Set of Groups.  If the set is null or empty, nothing will be done.
     * @param user
     * @param groups
     * @throws FindException
     * @throws UpdateException
     */
    void removeUser( User user, Set<Group> groups ) throws FindException, UpdateException;

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
    Set<IdentityHeader> getGroupHeaders( User user ) throws FindException;

    Set<IdentityHeader> getGroupHeaders( String userId ) throws FindException;

    void setGroupHeaders( User user, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;
    void setGroupHeaders( String userId, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;

    /**
     * Retrieve the Set of Users belonging to a particular Group.
     * @param group
     * @return
     * @throws FindException
     */
    Set<IdentityHeader> getUserHeaders( Group group ) throws FindException;
    Set<IdentityHeader> getUserHeaders( String groupId ) throws FindException;

    void setUserHeaders( Group group, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;
    void setUserHeaders( String groupId, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;

    Collection<IdentityHeader> findAllHeaders() throws FindException;

    Collection findAll() throws FindException;
}
