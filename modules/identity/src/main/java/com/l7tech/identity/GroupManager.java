package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * @author alex
 */
public interface GroupManager<UT extends User, GT extends Group> {
    GT findByPrimaryKey( String identifier ) throws FindException;
    void delete(GT group) throws DeleteException;
    void delete(String identifier) throws DeleteException, ObjectNotFoundException;
    void deleteAll( Goid ipoid ) throws DeleteException, ObjectNotFoundException;
    void deleteAllVirtual( Goid ipoid ) throws DeleteException, ObjectNotFoundException;
    String saveGroup(GT group) throws SaveException;
    void update(GT group) throws UpdateException, FindException;
    String save(GT group, Set<IdentityHeader> userHeaders ) throws SaveException;
    String save(Goid id, GT group, Set<IdentityHeader> userHeaders ) throws SaveException;
    void update(GT group, Set<IdentityHeader> userHeaders ) throws UpdateException, FindException;
    Collection<IdentityHeader> search(String searchString) throws FindException;
    Class getImpClass();
    GT reify(GroupBean bean);

    /**
     * Test whether a given User is a member of the specified Group.
     * @param user
     * @param group
     * @throws FindException
     */
    boolean isMember(User user, GT group) throws FindException;

    /**
     * Add a setInstance of Users to a single Group.  If the set is null or empty, nothing is done.
     * @param group
     * @param users
     * @throws FindException
     * @throws UpdateException
     */
    void addUsers(GT group, Set<UT> users) throws FindException, UpdateException;

    /**
     * Remove a setInstance of Users from a single Group.  If the set is null or empty, nothing is done.
     * @param group
     * @param users
     * @throws FindException
     * @throws UpdateException
     */
    void removeUsers(GT group, Set<UT> users ) throws FindException, UpdateException;

    /**
     * Add a single User to a Set of Groups.  If the set is null or empty, nothing will be done.
     * @param user
     * @param groups
     * @throws FindException
     * @throws UpdateException
     */
    void addUser(UT user, Set<GT> groups ) throws FindException, UpdateException;

    /**
     * Remove a single User from a Set of Groups.  If the set is null or empty, nothing will be done.
     * @param user
     * @param groups
     * @throws FindException
     * @throws UpdateException
     */
    void removeUser(UT user, Set<GT> groups) throws FindException, UpdateException;

    /**
     * Add a single User to a single Group.
     * @param user
     * @param group
     * @throws FindException
     * @throws UpdateException
     */
    void addUser(UT user, GT group) throws FindException, UpdateException;

    /**
     * Remove a single User from a single Group.
     * @param user
     * @param group
     * @throws FindException
     * @throws UpdateException
     */
    void removeUser(UT user, GT group ) throws FindException, UpdateException;

    /**
     * Retrieve the Set of Groups to which a particular User belongs.
     * @param user
     * @throws FindException
     */
    Set<IdentityHeader> getGroupHeaders(UT user) throws FindException;

    Set<IdentityHeader> getGroupHeaders(String userId) throws FindException;

    /**
     * Retrieve the set of groups to which a particular group belongs.
     * @param groupId the id of the group which may belong to other groups (is nested).
     * @return a set of groups to which the given group belongs. If nested groups are not supported, returns an empty collection.
     * @throws FindException
     */
    @NotNull
    Set<IdentityHeader> getGroupHeadersForNestedGroup(@NotNull final String groupId) throws FindException;

    void setGroupHeaders(UT user, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;
    void setGroupHeaders(String userId, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;

    /**
     * Retrieve the Set of Users belonging to a particular Group.
     * @param group
     * @throws FindException
     */
    Set<IdentityHeader> getUserHeaders(GT group ) throws FindException;
    Set<IdentityHeader> getUserHeaders(String groupId) throws FindException;

    void setUserHeaders(GT group, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;
    void setUserHeaders(String groupId, Set<IdentityHeader> groupHeaders ) throws FindException, UpdateException;

    EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException;

    Collection findAll() throws FindException;

    GT findByName(String groupName) throws FindException;
}
