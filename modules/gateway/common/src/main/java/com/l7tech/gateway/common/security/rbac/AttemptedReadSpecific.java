/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

/**
 * @author alex
 */
public class AttemptedReadSpecific extends AttemptedOperation {
    private final String id;

    public AttemptedReadSpecific(EntityType type, String id) {
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
