/*
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Manages persistent {@link Role} instances.  Primarily used by {@link RbacAdminImpl}.
 */
public interface RoleManager extends EntityManager<Role, EntityHeader> {
    public static final String ADMIN_REQUIRED = "At least one User must always be assigned to the Administrator role";

    @Transactional(readOnly=true)
    Collection<Role> getAssignedRoles(User user) throws FindException;

    @Transactional(readOnly=true)
    boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException;

    /**
     * Returns <em>true</em> if the specified operation is permitted against any entity of the specified type; false otherwise.
     * @param authenticatedUser the User who was authenticated; must not be null
     * @param requiredOperation the operation required; must be non-null and a member of {@link com.l7tech.common.security.rbac.OperationType#ALL_CRUD}
     * @param requiredType the type against which the operation must be permitted; must not be null or empty
     * @return true if the specified operation is permitted for any entity of the specified type; false otherwise
     * @throws FindException in the event of a database problem
     */
    @Transactional(readOnly=true)
    boolean isPermittedForAnyEntityOfType(User authenticatedUser, OperationType requiredOperation, EntityType requiredType) throws FindException;

    /**
     * Finds the first role in which every {@link com.l7tech.common.security.rbac.Permission}
     * matches the provided callback.
     *
     * @param callback callback that determines if a candidate Permission matches
     * @return the Role in question, or null if none was found.
     * @throws FindException if there was a problem accessing the database
     */
    @Transactional(readOnly=true)
    Role findEntitySpecificRole(PermissionMatchCallback callback) throws FindException;

    /**
     * Finds the first role in which every {@link com.l7tech.common.security.rbac.Permission} is scoped to the single
     * provided {@link Entity}.
     *
     * @param etype
     * @param entity the Entity that is being sought.  must not be null. @return the Role in question, or null if none was found.
     * @throws FindException if there was a problem accessing the database
     */
    @Transactional(readOnly=true)
    Role findEntitySpecificRole(EntityType etype, PersistentEntity entity) throws FindException;

    /**
     * Returns true if the specified operation is permitted for <em>all</em> of the specified types; false otherwise
     *
     * @param authenticatedUser the User who was authenticated; must not be null
     * @param requiredOperation the operation required; must be non-null and a member of {@link com.l7tech.common.security.rbac.OperationType#ALL_CRUD}
     * @param requiredTypes the Set of types against which the operation must be permitted; must not be null or empty
     * @return true if the specified operation is permitted for <em>all</em> of the specified types; false otherwise
     * @throws com.l7tech.objectmodel.FindException in the event of a database problem
     */
    @Transactional(readOnly=true)
    boolean isPermittedForEntitiesOfTypes(User authenticatedUser, OperationType requiredOperation, Set<EntityType> requiredTypes) throws FindException;

    /**
     * Deletes any roles in which every {@link com.l7tech.common.security.rbac.Permission} matches the provided callback.
     *
     * @param callback   callback that determines if a candidate Permission matches.
     * @throws com.l7tech.objectmodel.DeleteException
     */
    void deleteEntitySpecificRole(PermissionMatchCallback callback) throws DeleteException;

    /**
     * Deletes any roles in which every {@link com.l7tech.common.security.rbac.Permission} is scoped to the single
     * provided {@link Entity} (which is presumably being deleted).
     *
     * @param etype
     * @param entity the Entity that is being deleted @throws com.l7tech.objectmodel.DeleteException
     */
    void deleteEntitySpecificRole(EntityType etype, PersistentEntity entity) throws DeleteException;

    /**
     * Updates the Role corresponding to the provided Entity to match a new name, if it's different
     * @param entityType the RBAC type of the Entity being updated
     * @param entity the entity being updated
     * @param replacePattern a Pattern that finds the name component in the entity name
     * @throws com.l7tech.objectmodel.UpdateException
     */
    void renameEntitySpecificRole(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException;
}
