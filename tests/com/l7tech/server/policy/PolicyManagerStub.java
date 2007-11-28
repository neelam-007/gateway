/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

/**
 * @author alex
*/
public class PolicyManagerStub extends EntityManagerStub<Policy> implements PolicyManager {
    public PolicyManagerStub(Policy[] entities) {
        super(entities);
    }

    public PolicyManagerStub() {
        super(new Policy[0]);
    }

    public Collection<EntityHeader> findHeadersByType(PolicyType type) throws FindException {
        Set<EntityHeader> hs = new HashSet<EntityHeader>();
        for (Policy policy : entities.values()) {
            if (policy.getType() == type) hs.add(new EntityHeader(policy.getId(), EntityType.POLICY, policy.getName(), null));
        }
        return hs;
    }

    public void addManagePolicyRole(Policy policy) throws SaveException {
        // No-op for stub mode
    }
}
