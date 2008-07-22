/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 */
public abstract class AttemptedEntityOperation extends AttemptedOperation {
    protected final Entity entity;

    protected AttemptedEntityOperation(EntityType type, Entity entity) {
        super(type);
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
