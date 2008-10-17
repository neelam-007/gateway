/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityType;

/**
 * User is allowed to invoke the operation if s/he has blanket permission to read
 * <em>all</em> instances of the type.
 * @author alex
 */
public class AttemptedReadAll extends AttemptedOperation {
    public AttemptedReadAll(EntityType type) {
        super(type);
    }

    public OperationType getOperation() {
        return OperationType.READ;
    }
}
