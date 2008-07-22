/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

/**
 * @author alex
 */
public class AttemptedAnyOperation extends AttemptedOperation {
    public AttemptedAnyOperation(EntityType type) {
        super(type);
    }

    public OperationType getOperation() {
        return OperationType.NONE;
    }
}
