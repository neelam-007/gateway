/*
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Manages persistent {@link Role} instances.  Primarily used by {@link RbacAdminImpl}.
 *
 * TODO de-implement RbacServices when it becomes tractable
 */
public interface RoleManager extends EntityManager<Role, EntityHeader>, RbacServices {
    public static final String ADMIN_REQUIRED = "At least one User must always be assigned to the Administrator role";

    @Transactional(readOnly=true)
    Collection<Role> getAssignedRoles(User user) throws FindException;

    /**
     * Find a role by tag.
     *
     * @return The role or null if non was found.
     */
    @Transactional(readOnly=true)
    Role findByTag(Role.Tag tag) throws FindException;

    /**
     * Finds the first role in which every {@link com.l7tech.gateway.common.security.rbac.Permission} is scoped to the single
     * provided {@link Entity}.
     */
    @Transactional(readOnly=true)
    Collection<Role> findEntitySpecificRoles(EntityType etype, long entityOid) throws FindException;

    /**
     * Deletes any roles in which every {@link com.l7tech.gateway.common.security.rbac.Permission} is scoped to the single
     * provided {@link Entity} (which is presumably being deleted).
     *
     * @param etype
     * @param entityOid
     */
    void deleteEntitySpecificRoles(EntityType etype, long entityOid) throws DeleteException;

    /**
     * Updates the Roles corresponding to the provided Entity to match a new name, if it's different
     * @param entityType the RBAC type of the Entity being updated
     * @param entity the entity being updated
     * @param replacePattern a Pattern that finds the name component in the entity name
     * @throws com.l7tech.objectmodel.UpdateException
     */
    void renameEntitySpecificRoles(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException;

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
}
