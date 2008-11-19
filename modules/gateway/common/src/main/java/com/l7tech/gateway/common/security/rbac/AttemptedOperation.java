/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

import java.io.Serializable;

/**
 * @author alex
 */
public abstract class AttemptedOperation implements Serializable {
    private final EntityType type;

    public EntityType getType() {
        return type;
    }

    public abstract OperationType getOperation();

    public AttemptedOperation(EntityType type) {
        this.type = type;
    }
}
