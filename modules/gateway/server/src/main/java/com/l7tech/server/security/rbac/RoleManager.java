/*
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Manages persistent {@link Role} instances.  Primarily used by {@link RbacAdminImpl}.
 *
 * TODO de-implement RbacServices when it becomes tractable
 */
public interface RoleManager extends GoidEntityManager<Role, EntityHeader>, RbacServices {
    public static final String ADMIN_REQUIRED = "At least one enabled User with no expiry must always be assigned to the Administrator role";

    @Transactional(readOnly=true)
    Collection<Role> getAssignedRoles(User user) throws FindException;

    /**
     * Get Roles that a Group is assigned to.
     *
     * @param group the group for which to retrieve roles it is assigned to.
     * @return a list of Roles which the given Group is assigned to.
     * @throws FindException if a db error occurs retrieving the roles.
     */
    @Transactional(readOnly = true)
    Collection<Role> getAssignedRoles(@NotNull final Group group) throws FindException;

    /**
     * Finds the roles assigned to this user, optionally skipping validation of the account for expiry, disablement etc.
     *
     * @param user The user whose roles should be accessed
     * @param skipAccountValidation True to skip account validation. This is only for display purposes -- don't use for authorization!
     * @param disabledGroupIsError True if an exception should be thrown if there are no roles assigned and one or more disabled groups.
     * @throws DisabledGroupRolesException if disabledGroupIsError is true an the user has no role assignements due to disabled groups.
     */
    @Transactional(readOnly=true)
    Collection<Role> getAssignedRoles(User user, boolean skipAccountValidation, boolean disabledGroupIsError) throws FindException;

    /**
     * Get the distinct Collection of users who have a direct role assignment.
     * @return Collection of Pairs. Each pair has the providerId as left, and the user unique id as right. Note for
     * internal users this is an oid for external users it is a CN.
     * @throws FindException
     */
    @Transactional(readOnly=true)
    public Collection<Pair<Goid, String>> getExplicitRoleAssignments() throws FindException;

    /**
     * Find a role by tag.
     *
     * @return The role or null if non was found.
     */
    @Transactional(readOnly=true)
    Role findByTag(Role.Tag tag) throws FindException;

    /**
     * Finds all roles that are marked as being scoped to a particular entity instance of a particular type,
     * using the metadata in {@link com.l7tech.gateway.common.security.rbac.Role#getEntityOid()} and
     * {@link com.l7tech.gateway.common.security.rbac.Role#getEntityType()}.
     * <p/>
     * This method does <b>not</b> actually check the scopes of all permissions of all roles in order
     * to find a match.
     */
    @Transactional(readOnly=true)
    @Deprecated
    Collection<Role> findEntitySpecificRoles(EntityType etype, long entityOid) throws FindException;

    /**
     * Finds all roles that are marked as being scoped to a particular entity instance of a particular type,
     * using the metadata in {@link com.l7tech.gateway.common.security.rbac.Role#getEntityGoid()} and
     * {@link com.l7tech.gateway.common.security.rbac.Role#getEntityType()}.
     * <p/>
     * This method does <b>not</b> actually check the scopes of all permissions of all roles in order
     * to find a match.
     */
    @Transactional(readOnly=true)
    Collection<Role> findEntitySpecificRoles(EntityType etype, Goid entityOid) throws FindException;

    /**
     * Deletes all roles that are marked as being scoped to a particular entity instance (which is presumably being deleted),
     * using the metadata in {@link com.l7tech.gateway.common.security.rbac.Role#getEntityOid()} and
     * {@link com.l7tech.gateway.common.security.rbac.Role#getEntityType()}.
     * <p/>
     * This method also scans for and UPDATES any roles (that aren't being deleted outright) that contain permissions
     * with at least one scope dependent upon the specified entity.  Any affected permissions will be removed from the
     * affected roles.  It is possible that this may leave roles that contain no permissions.
     *
     * @param etype type of entity whose roles are to be deleted, eg Folder.  Required.
     * @param entityOid OID of entity instnace whose roles are to be deleted.  Required.
     */
    @Deprecated
    void deleteEntitySpecificRoles(EntityType etype, long entityOid) throws DeleteException;

    /**
     * Deletes all roles that are marked as being scoped to a particular entity instance (which is presumably being deleted),
     * using the metadata in {@link com.l7tech.gateway.common.security.rbac.Role#getEntityGoid()} and
     * {@link com.l7tech.gateway.common.security.rbac.Role#getEntityType()}.
     * <p/>
     * This method also scans for and UPDATES any roles (that aren't being deleted outright) that contain permissions
     * with at least one scope dependent upon the specified entity.  Any affected permissions will be removed from the
     * affected roles.  It is possible that this may leave roles that contain no permissions.
     *
     * @param etype type of entity whose roles are to be deleted, eg Folder.  Required.
     * @param entityGoid GOID of entity instnace whose roles are to be deleted.  Required.
     */
    void deleteEntitySpecificRoles(EntityType etype, Goid entityGoid) throws DeleteException;

    /**
     * Updates the Roles corresponding to the provided Entity to match a new name, if it's different
     * @param entityType the RBAC type of the Entity being updated
     * @param entity the entity being updated
     * @param replacePattern a Pattern that finds the name component in the entity name
     * @throws com.l7tech.objectmodel.UpdateException
     */
    @Deprecated
    void renameEntitySpecificRoles(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException;

    /**
     * Updates the Roles corresponding to the provided Entity to match a new name, if it's different
     * @param entityType the RBAC type of the Entity being updated
     * @param entity the entity being updated
     * @param replacePattern a Pattern that finds the name component in the entity name
     * @throws com.l7tech.objectmodel.UpdateException
     */
    void renameEntitySpecificRoles(EntityType entityType, NamedGoidEntityImp entity, Pattern replacePattern) throws FindException, UpdateException;

    /**
     * Ensure that the assignment of a users to roles is acceptable.
     *
     * @throws UpdateException If there is not an acceptable role assignment.
     */
    void validateRoleAssignments() throws UpdateException;

    /**
     * Deletes the role assignments for the user.
     *
     * @param user  The user whose role assignments will be deleted.
     * @throws DeleteException
     */
    void deleteRoleAssignmentsForUser(final User user) throws DeleteException;


    /**
     * Deletes the role assignments for the group.
     *
     * @param group  The group whose role assignments will be deleted.
     * @throws DeleteException
     */
    void deleteRoleAssignmentsForGroup(final Group group) throws DeleteException;

    /**
     * Exception thrown when a user has no role assignments due to disabled groups.
     */
    class DisabledGroupRolesException extends FindException {
        public DisabledGroupRolesException( final String message ) {
            super( message );
        }
    }
}
