package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;

/**
 * User is allowed to invoke the operation if s/he is permitted to update <em>specific</em> entity
 * instance of the type.
 */
public class AttemptedOtherSpecific extends AttemptedEntityOperation {
    private final String otherOperationName;

    /**
     *  Creates <code>AttemptedOtherSpecific</code>.
     *
     * @param type the entity type
     * @param entity the entity
     * @param otherOperationName the other operation name
     */
    public AttemptedOtherSpecific(EntityType type, Entity entity, String otherOperationName) {
        super(type, entity);
        this.otherOperationName = otherOperationName;
    }

    /**
     * Gets the other operation name.
     *
     * @return the other operation name
     */
    public String getOtherOperationName() {
        return otherOperationName;
    }

    @Override
    public OperationType getOperation() {
        return OperationType.OTHER;
    }
}