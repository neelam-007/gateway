/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;

/**
 * Reflects an attempt to create an entity *like* the one passed to the constructor
 * @author alex
 */
public class AttemptedCreateSpecific extends AttemptedEntityOperation {
    /**
     * The provided entity should be as similar as possible to those that will be created if this attempt is deemed
     * permitted.  Specifically, any field that might be the subject of an {@link AttributePredicate} should have a
     * value.
     */
    public AttemptedCreateSpecific(EntityType type, Entity entity) {
        super(type, entity);
    }

    public OperationType getOperation() {
        return OperationType.CREATE;
    }
}
