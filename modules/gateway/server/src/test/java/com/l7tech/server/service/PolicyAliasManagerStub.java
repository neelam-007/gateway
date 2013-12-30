/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.server.policy.PolicyAliasManager;

/** @author alex */
public class PolicyAliasManagerStub extends AliasManagerStub<PolicyAlias, Policy, PolicyHeader> implements PolicyAliasManager {
    public PolicyAliasManagerStub(PolicyAlias... entitiesIn) {
        super(entitiesIn);
    }

    @Override
    public PolicyHeader getNewEntityHeader(PolicyHeader policyHeader) {
        return new PolicyHeader(policyHeader);
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return PolicyAlias.class;
    }
}
