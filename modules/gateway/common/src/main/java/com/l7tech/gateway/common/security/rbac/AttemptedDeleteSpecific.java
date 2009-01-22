/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;

/**
 * @author alex
 */
public class AttemptedDeleteSpecific extends AttemptedEntityOperation {

    public AttemptedDeleteSpecific(EntityType type, Entity entity) {
        super(type, entity);
    }

    @Override
    public OperationType getOperation() {
        return OperationType.DELETE;
    }
}
