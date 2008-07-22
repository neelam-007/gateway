/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

/**
 * @author alex
 */
public class NullAttemptedOperation extends AttemptedOperation {
    public NullAttemptedOperation() {
        super(null);
    }

    public OperationType getOperation() {
        return OperationType.OTHER;
    }
}
