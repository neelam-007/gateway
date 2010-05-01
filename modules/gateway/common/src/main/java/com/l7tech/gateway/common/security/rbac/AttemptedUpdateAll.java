package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

/**
 *
 */
public class AttemptedUpdateAll extends AttemptedOperation {
    public AttemptedUpdateAll(EntityType type) {
        super(type);
    }

    @Override
    public OperationType getOperation() {
        return OperationType.UPDATE;
    }
}
