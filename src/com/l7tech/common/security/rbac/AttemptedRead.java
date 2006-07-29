/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.objectmodel.Entity;

/**
 * @author alex
 */
public class AttemptedRead extends AttemptedOperation {
    private final long oid;
    private final String uniqueValue;

    public AttemptedRead(EntityType type, long oid) {
        super(type);
        this.oid = oid;
        this.uniqueValue = null;
    }

    public AttemptedRead(EntityType type, String unique) {
        super(type);
        this.uniqueValue = unique;
        this.oid = Entity.DEFAULT_OID;
    }

    public long getOid() {
        return oid;
    }

    public String getUniqueValue() {
        return uniqueValue;
    }

    public OperationType getOperation() {
        return OperationType.READ;
    }
}
