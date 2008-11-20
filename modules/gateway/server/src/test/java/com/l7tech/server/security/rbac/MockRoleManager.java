package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.server.EntityFinder;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 */
public class MockRoleManager extends EntityManagerStub<Role,EntityHeader> implements RoleManager {
    @Override
    public Collection<Role> getAssignedRoles(User user) throws FindException {
        return null;
    }

    @Override
    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException {
        return false;
    }

    @Override
    public boolean isPermittedForAnyEntityOfType(User authenticatedUser, OperationType requiredOperation, EntityType requiredType) throws FindException {
        return false;
    }

    @Override
    public Role findByTag(Role.Tag tag) throws FindException {
        return null;
    }

    @Override
    public Role findEntitySpecificRole(PermissionMatchCallback callback) throws FindException {
        return null;
    }

    @Override
    public Role findEntitySpecificRole(EntityType etype, long entityOid) throws FindException {
        return null;
    }

    @Override
    public boolean isPermittedForEntitiesOfTypes(User authenticatedUser, OperationType requiredOperation, Set<EntityType> requiredTypes) throws FindException {
        return false;
    }

    @Override
    public void deleteEntitySpecificRole(PermissionMatchCallback callback) throws DeleteException {
    }

    @Override
    public void deleteEntitySpecificRole(EntityType etype, long entityOid) throws DeleteException {
    }

    @Override
    public void renameEntitySpecificRole(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException {
    }

    @Override
    public <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders(User authenticatedUser, OperationType requiredOperation, Iterable<T> headers, EntityFinder entityFinder) throws FindException {
        return null;
    }

    @Override
    public void validateRoleAssignments() throws UpdateException {
    }

    @Override
    public void deleteRoleAssignmentsForUser(User user) throws DeleteException {
    }
}
