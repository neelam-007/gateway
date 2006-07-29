/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 */
public class AttemptedUpdate extends AttemptedEntityOperation {
    public AttemptedUpdate(EntityType type, Entity entity) {
        super(type, entity);
    }

    public OperationType getOperation() {
        return OperationType.UPDATE;
    }
}
