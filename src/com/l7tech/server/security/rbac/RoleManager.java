/*
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.User;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.UserRoleAssignment;

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

    boolean isPermittedOperation(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException;

    void deleteAssignment(User user, Role role) throws UpdateException;
}
