package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Permissive RbacServices / SecurityFilter stub
 */
public class RbacServicesStub implements RbacServices, SecurityFilter {

    @Override
    public boolean isPermittedForEntitiesOfTypes( final User authenticatedUser, final OperationType requiredOperation, final Set<EntityType> requiredTypes ) throws FindException {
        return true;
    }

    @Override
    public boolean isPermittedForAnyEntityOfType( final User authenticatedUser, final OperationType requiredOperation, final EntityType requiredType ) throws FindException {
        return true;
    }

    @Override
    public boolean isPermittedForSomeEntityOfType( final User authenticatedUser, final OperationType requiredOperation, final EntityType requiredType ) throws FindException {
        return true;
    }

    @Override
    public boolean isPermittedForEntity( final User user, final Entity entity, final OperationType operation, final String otherOperationName ) throws FindException {
        return true;
    }

    @Override
    public <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders( final User authenticatedUser, final OperationType requiredOperation, final Iterable<T> headers, final EntityFinder entityFinder ) throws FindException {
        return headers;
    }

    @Override
    public boolean isAdministrativeUser(@NotNull Pair<Goid, String> providerAndUserId, @NotNull User user) throws FindException {
        return true;
    }

    @Override
    public Collection<Role> getAssignedRoles(@NotNull Pair<Goid, String> providerAndUserId, @NotNull User user) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public <T> Collection<T> filter( final Collection<T> entityCollection, final User user, final OperationType type, final String operationName ) throws FindException {
        return entityCollection;
    }
}
