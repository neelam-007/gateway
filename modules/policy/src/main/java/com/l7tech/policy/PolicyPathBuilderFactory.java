/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.objectmodel.HeaderBasedEntityFinder;
import com.l7tech.policy.Policy;
import com.l7tech.objectmodel.GuidBasedEntityManager;

/**
 * @author alex
 */
public class PolicyPathBuilderFactory {
    private final GuidBasedEntityManager<Policy> policyFinder;
    private final HeaderBasedEntityFinder entityFinder;

    public PolicyPathBuilderFactory(GuidBasedEntityManager<Policy> policyFinder, HeaderBasedEntityFinder entityFinder) {
        this.policyFinder = policyFinder;
        this.entityFinder = entityFinder;
    }

    public PolicyPathBuilder makePathBuilder() {
        return new DefaultPolicyPathBuilder(policyFinder, entityFinder);
    }
}
