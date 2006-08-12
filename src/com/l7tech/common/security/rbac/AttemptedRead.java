/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

/**
 * @author alex
 */
public class AttemptedRead extends AttemptedOperation {
    private final String id;

    public AttemptedRead(EntityType type, String id) {
        super(type);
        this.id = id;
    }

    public OperationType getOperation() {
        return OperationType.READ;
    }

    public String getId() {
        return id;
    }
}
