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
    private final long oid;

    public AttemptedDelete(EntityType type, Entity entity) {
        super(type, entity);
        this.oid = entity.getOid();
    }

    public AttemptedDelete(EntityType type, long oid) {
        super(type, new AnonymousEntityReference(type.getEntityClass(), oid));
        this.oid = oid;
    }

    public long getOid() {
        return oid;
    }

    public OperationType getOperation() {
        return OperationType.DELETE;
    }
}
