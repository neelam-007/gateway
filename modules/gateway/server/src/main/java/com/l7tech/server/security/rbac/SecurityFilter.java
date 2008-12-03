package com.l7tech.server.security.rbac;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.security.rbac.OperationType;

import java.util.List;
import java.util.Collection;

/**
 * Entity filter for security enforcement.
 */
public interface SecurityFilter {

    /**
     * Filter the given collection to contain only entities that are visible for the user.
     *
     * @param entityCollection The collection of entities or entity headers to filter.
     * @param user The user to filter for
     * @param type The operation type
     * @param operationName The operation name
     * @return The filtered entity collection
     * @throws FindException If an error occurs determining security permissions.
     */
    <T> Collection<T> filter( Collection<T> entityCollection, User user, OperationType type, String operationName ) throws FindException;

}
