/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

/**
 * User is allowed to invoke the operation if s/he is permitted to read <em>any</em>
 * instance of the type.
 */
public class AttemptedReadAny extends AttemptedOperation {
    public AttemptedReadAny(EntityType type) {
        super(type);
    }

    public OperationType getOperation() {
        return OperationType.READ;
    }
}
