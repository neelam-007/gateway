/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;

import java.text.MessageFormat;

/**
 * Thrown by the SSG when an admin API attempts an operation for which the caller does not have
 * permission.
 *
 * @author alex
 */
public class PermissionDeniedException extends RuntimeException {
    private final OperationType operation;
    private final String otherOperationName;
    private final Entity entity;
    private final EntityType type;

    public PermissionDeniedException(OperationType operation, Entity entity) {
        this(operation, entity, null);
    }

    public PermissionDeniedException(OperationType operation, EntityType type) {
        super(MessageFormat.format("Permission denied: {0} {1}", operation, type));
        this.operation = operation;
        this.type = type;
        this.entity = null;
        this.otherOperationName = null;
    }

    public PermissionDeniedException(OperationType operation, EntityType type, String extraMessage) {
        super(MessageFormat.format("Permission denied: {0} {1} ({2})", operation, type, extraMessage));
        this.operation = operation;
        this.type = type;
        this.entity = null;
        this.otherOperationName = null;
    }

    public PermissionDeniedException(OperationType operation, Entity entity, String otherName) {
        super(MessageFormat.format("Permission denied: {0} {1}",
                operation == OperationType.OTHER ? otherName : operation, entity));
        this.operation = operation;
        this.entity = entity;
        this.otherOperationName = otherName;
        this.type = null;
    }

    public EntityType getType() {
        return type;
    }

    public OperationType getOperation() {
        return operation;
    }

    public String getOtherOperationName() {
        return otherOperationName;
    }

    public Entity getEntity() {
        return entity;
    }
}
