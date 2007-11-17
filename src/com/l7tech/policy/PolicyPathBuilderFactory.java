/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ReadOnlyEntityManager;

/**
 * @author alex
 */
public class PolicyPathBuilderFactory {
    private final ReadOnlyEntityManager<Policy, EntityHeader> policyFinder;

    public PolicyPathBuilderFactory(ReadOnlyEntityManager<Policy, EntityHeader> policyFinder) {
        this.policyFinder = policyFinder;
    }

    public PolicyPathBuilder makePathBuilder() {
        return new DefaultPolicyPathBuilder(policyFinder);
    }
}
