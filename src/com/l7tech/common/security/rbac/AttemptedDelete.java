/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.AnonymousEntityReference;

/**
 * @author alex
 */
public class AttemptedDelete extends AttemptedEntityOperation {
    private final String id;

    public AttemptedDelete(EntityType type, Entity entity) {
        super(type, entity);
        this.id = entity.getId();
    }

    public AttemptedDelete(EntityType type, String id) {
        super(type, new AnonymousEntityReference(type.getEntityClass(), id));
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public OperationType getOperation() {
        return OperationType.DELETE;
    }
}
