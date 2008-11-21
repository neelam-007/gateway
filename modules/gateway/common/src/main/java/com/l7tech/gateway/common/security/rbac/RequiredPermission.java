package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

/**
 * An individual required permission.
 */
public @interface RequiredPermission {

    /**
     * The target entity type of this permission.
     *
     * @return The entity type.
     */
    EntityType entityType();

    /**
     * The operation that is attempted on the target entity.
     *
     * @return The operation type.
     */
    OperationType operationType();
}
