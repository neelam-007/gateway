package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions.InsufficientPermissionsException;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * The rbac access service will check if the current user has access to certain operations
 */
public class RbacAccessService {

    @Inject
    private RbacServices rbacServices;
    @Inject
    private SecurityFilter securityFilter;

    /**
     * Check if a user is permitted to perform a given operation on the given entity.
     *
     * @param entity        The entity that check access on.
     * @param operationType The operation type
     * @throws InsufficientPermissionsException This is thrown if the current user does not have sufficient privileges
     *                                          to perform the operation of the entity
     */
    public void validatePermitted(@NotNull final Entity entity,
                                  @NotNull final OperationType operationType) {
        validatePermitted(entity, operationType, null);
    }

    /**
     * Check if a user is permitted to perform a given operation on the given entity. Here the operation type will be
     * set to {@link com.l7tech.gateway.common.security.rbac.OperationType#OTHER}
     *
     * @param entity             The entity that check access on.
     * @param otherOperationName The other operation name.
     * @throws InsufficientPermissionsException This is thrown if the current user does not have sufficient privileges
     *                                          to perform the operation of the entity
     */
    public void validatePermitted(@NotNull final Entity entity,
                                  @NotNull final String otherOperationName) {
        validatePermitted(entity, OperationType.OTHER, otherOperationName);
    }

    /**
     * Check if a user is permitted to perform a given operation on the given entity type. Here the operation type will be
     * set to {@link com.l7tech.gateway.common.security.rbac.OperationType#OTHER}
     *
     * @param entityType    The entity type to check access on.
     * @param operationType The operation type
     * @throws InsufficientPermissionsException This is thrown if the current user does not have sufficient privileges
     *                                          to perform the operation of the entity
     */
    public void validatePermitted(@NotNull final EntityType entityType,
                                  @NotNull final OperationType operationType) {
        final User user = getCurrentUser();
        try {
            if (!rbacServices.isPermittedForAnyEntityOfType(user, operationType, entityType)) {
                throw new InsufficientPermissionsException(user, entityType, operationType, null);
            }
        } catch (FindException e) {
            throw new InsufficientPermissionsException(user, entityType, operationType, null, e);
        }
    }

    /**
     * Check if a user is permitted to perform a given operation on the given entity.
     *
     * @param entity             The entity that check access on.
     * @param operationType      The operation type
     * @param otherOperationName The other operation name. Can be null
     * @throws InsufficientPermissionsException This is thrown if the current user does not have sufficient privileges
     *                                          to perform the operation of the entity
     */
    private void validatePermitted(@NotNull final Entity entity,
                                   @NotNull final OperationType operationType,
                                   @Nullable final String otherOperationName) {
        final User user = getCurrentUser();
        try {
            if (!rbacServices.isPermittedForEntity(user, entity, operationType, otherOperationName)) {
                throw new InsufficientPermissionsException(user, entity, operationType, otherOperationName);
            }
        } catch (FindException e) {
            throw new InsufficientPermissionsException(user, entity, operationType, otherOperationName, e);
        }
    }

    /**
     * This will check if the current user has the Administrator role.
     *
     * @throws InsufficientPermissionsException This is thrown if the current user is not an Administrator
     */
    public void validateFullAdministrator() {
        final User user = getCurrentUser();
        try {
            for (final Role role : rbacServices.getAssignedRoles(new Pair<>(user.getProviderId(), user.getId()), user)) {
                if (Role.Tag.ADMIN.equals(role.getTag())) {
                    return;
                }
            }
        } catch (FindException e) {
            throw new InsufficientPermissionsException("Permission denied for user: " + user.getLogin() + " Could not find user roles.", e);
        }
        throw new InsufficientPermissionsException("Permission denied for user: " + user.getLogin() + " does not have administrator role.");
    }

    /**
     * Gets the current user or throws an exception
     *
     * @return The currently authenticated user
     * @throws InsufficientPermissionsException This is thrown if there is no currently authenticated user
     */
    @NotNull
    private User getCurrentUser() {
        final User user = JaasUtils.getCurrentUser();
        if (user == null) {
            throw new InsufficientPermissionsException("No user currently authenticated.");
        }
        return user;
    }

    /**
     * Filter a collection of entities or entity types given a users access.
     *
     * @param entities           The entities list to filter
     * @param entityType         The type of entities in the list
     * @param operationType      The operation type that will be performed on the entities
     * @param otherOperationName The other operation name if the operation type is {@link com.l7tech.gateway.common.security.rbac.OperationType#OTHER}
     * @return The filtered list of entities
     */
    final public <ET> List<ET> accessFilter(@NotNull final List<ET> entities,
                                            @NotNull final EntityType entityType,
                                            @NotNull final OperationType operationType,
                                            @Nullable final String otherOperationName) {
        final List<ET> filteredEntities;
        final User user = JaasUtils.getCurrentUser();

        if (user != null) {
            try {
                if (rbacServices.isPermittedForAnyEntityOfType(user, operationType, entityType)) {
                    filteredEntities = entities;
                } else {
                    filteredEntities = securityFilter.filter(entities, user, operationType, otherOperationName);
                }
            } catch (FindException e) {
                throw new InsufficientPermissionsException("Could not filter entities for user: " + user.getLogin(), e);
            }
        } else {
            filteredEntities = Collections.emptyList();
        }

        return filteredEntities;
    }
}
