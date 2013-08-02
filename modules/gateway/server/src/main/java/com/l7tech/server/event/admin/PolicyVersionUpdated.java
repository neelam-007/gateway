package com.l7tech.server.event.admin;

import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.event.EntityChangeSet;

/**
 * Event fired when a policy version is changed.
 */
public class PolicyVersionUpdated extends Updated<PolicyVersion> {
    public PolicyVersionUpdated(PolicyVersion entity, EntityChangeSet changes) {
        super(entity, changes, getNote(entity, changes));
    }

    private static String getNote( PolicyVersion entity, EntityChangeSet changes) {
        if (entity.isActive()) {
            if (Boolean.FALSE.equals(changes.getOldValue("active"))) {
                // Policy version activated
                return "activated v" + entity.getOrdinal() + " of policy " + entity.getPolicyGoid();
            }
        }
        return null;
    }
}
