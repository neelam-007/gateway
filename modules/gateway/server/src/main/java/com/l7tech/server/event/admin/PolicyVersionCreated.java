package com.l7tech.server.event.admin;

import com.l7tech.policy.PolicyVersion;

/**
 * Event fired when a policy version is changed.
 */
public class PolicyVersionCreated extends Created<PolicyVersion> {
    public PolicyVersionCreated(PolicyVersion entity) {
        super(entity, getNote(entity));
    }

    private static String getNote( PolicyVersion entity) {
        if (entity.isActive())
            return "activated v" + entity.getOrdinal() + " of policy " + entity.getPolicyGoid();
        return null;
    }
}