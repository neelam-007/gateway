/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

/**
 * User is allowed to invoke the operation if s/he is permitted to update <em>any</em>
 * instance of the type.
 */
public class AttemptedUpdateAny extends AttemptedOperation {
    public AttemptedUpdateAny(EntityType type) {
        super(type);
    }

    public OperationType getOperation() {
        return OperationType.UPDATE;
    }
}