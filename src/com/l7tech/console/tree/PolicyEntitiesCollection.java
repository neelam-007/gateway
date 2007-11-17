/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.common.policy.PolicyAdmin;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.objectmodel.EntityHeader;

import java.util.Collection;
import java.util.Collections;

public class PolicyEntitiesCollection implements EntitiesCollection {
    final PolicyAdmin manager;
    private boolean exhausted = false;

    PolicyEntitiesCollection(PolicyAdmin pa) {
        manager = pa;
    }

    /**
     * @return Returns the collection of <code>EntityHeader</code> instances
     * @throws RuntimeException thrown on error retrieving the user collection
     */
    public Collection<EntityHeader> getNextBatch() throws RuntimeException {
        if (exhausted) {
            return Collections.emptyList();
        }
        try {
            exhausted = true;
            // TODO parameterize type as more policy types are implemented
            return manager.findPolicyHeadersByType(PolicyType.INCLUDE_FRAGMENT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
