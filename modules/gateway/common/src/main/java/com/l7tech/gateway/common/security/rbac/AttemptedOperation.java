/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

/**
 * @author alex
 */
public abstract class AttemptedOperation {
    private final EntityType type;

    public EntityType getType() {
        return type;
    }

    public abstract OperationType getOperation();

    public AttemptedOperation(EntityType type) {
        this.type = type;
    }
}
