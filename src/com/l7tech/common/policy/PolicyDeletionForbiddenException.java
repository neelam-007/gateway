/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.policy;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;

import java.text.MessageFormat;

/**
 * Thrown if an admin attempts to delete a policy that is referenced by some other entity.
 * This can currently only be another {@link Policy}.
 * 
 * @author alex
*/
public class PolicyDeletionForbiddenException extends Exception {
    private final Policy policy;
    private final EntityType referringEntityType;
    private final Entity referringEntity;

    public PolicyDeletionForbiddenException(Policy policy, EntityType referringEntityType, Entity referringEntity) {
        super(MessageFormat.format("Policy #{0} ({1}) cannot be deleted; it is referenced by {2} #{3} ({4})",
                policy.getOid(), policy.getName(),
                referringEntityType.getName(), referringEntity.getId(),
                referringEntity instanceof NamedEntity ? ((NamedEntity)referringEntity).getName() : null));
        this.policy = policy;
        this.referringEntityType = referringEntityType;
        this.referringEntity = referringEntity;
    }

    public Policy getPolicy() {
        return policy;
    }

    public EntityType getReferringEntityType() {
        return referringEntityType;
    }

    public Entity getReferringEntity() {
        return referringEntity;
    }
}
