package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.service.AliasManagerImpl;

/**
 * @author darmstrong
 */
public class PolicyAliasManagerImpl extends AliasManagerImpl<PolicyAlias, Policy, PolicyHeader>
        implements PolicyAliasManager
{
    public Class getImpClass() {
        return PolicyAlias.class;
    }

    public Class getInterfaceClass() {
        return PolicyAlias.class;
    }

    public String getTableName() {
        return "policy_alias";
    }

    public PolicyHeader getNewEntityHeader(PolicyHeader ph) {
        return new PolicyHeader(ph);
    }
}

