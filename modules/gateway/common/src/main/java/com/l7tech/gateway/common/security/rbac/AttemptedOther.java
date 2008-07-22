/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

/**
 * @author alex
 */
public class AttemptedOther extends AttemptedOperation {
    private final String otherOperationName;

    public AttemptedOther(EntityType type, String otherOperationName) {
        super(type);
        this.otherOperationName = otherOperationName;
    }

    public String getOtherOperationName() {
        return otherOperationName;
    }

    public OperationType getOperation() {
        return OperationType.OTHER;
    }
}
