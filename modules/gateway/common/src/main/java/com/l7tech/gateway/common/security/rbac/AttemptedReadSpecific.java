/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.AnonymousEntityReference;
import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 */
public class AttemptedReadSpecific extends AttemptedEntityOperation {

    public AttemptedReadSpecific(EntityType type, String id) {
        super(type, new AnonymousEntityReference(type.getEntityClass(), id));
    }

    public AttemptedReadSpecific(EntityType type, Entity entity) {
        super(type, entity);
    }

    @Override
    public OperationType getOperation() {
        return OperationType.READ;
    }
}
