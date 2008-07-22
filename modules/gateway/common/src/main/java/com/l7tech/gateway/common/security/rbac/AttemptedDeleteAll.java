/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

/**
 * @author alex
 */
public class AttemptedDeleteAll extends AttemptedOperation {
    public AttemptedDeleteAll(EntityType type) {
        super(type);
    }

    public OperationType getOperation() {
        return OperationType.DELETE;
    }
}
