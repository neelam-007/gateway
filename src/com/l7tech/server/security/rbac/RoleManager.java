/*
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.security.rbac.UserRoleAssignment;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Manages persistent {@link Role} instances.  Primarily used by {@link RbacAdminImpl}.
 */
public interface RoleManager extends EntityManager<Role, EntityHeader> {
    Collection<User> getAssignedUsers(Role role) throws FindException;

    Collection<UserRoleAssignment> getAssignments(User user) throws FindException;

    Collection<Role> getAssignedRoles(User user) throws FindException;

    void update(Role role) throws UpdateException;

    void assignUser(Role role, User user) throws UpdateException;

    boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException;

    void deleteAssignment(User user, Role role) throws UpdateException;

    @Transactional(readOnly=true)
    boolean isPermittedForAllEntities(User user, com.l7tech.common.security.rbac.EntityType type, OperationType operation) throws FindException;

    /**
     * Deletes any roles in which every {@link com.l7tech.common.security.rbac.Permission} matches the provided callback.
     * @param entity the Entity that is being deleted
     * @param callback 
     * @throws com.l7tech.objectmodel.FindException if
     * @throws com.l7tech.objectmodel.DeleteException
     */
    void deleteEntitySpecificRole(Entity entity, PermissionMatchCallback callback) throws DeleteException;

    /**
     * Deletes any roles in which every {@link com.l7tech.common.security.rbac.Permission} is scoped to the single
     * provided {@link Entity} (which is presumably being deleted)
     * @param entity the Entity that is being deleted
     * @throws com.l7tech.objectmodel.FindException if
     * @throws com.l7tech.objectmodel.DeleteException
     */
    void deleteEntitySpecificRole(Entity entity) throws DeleteException;
}
