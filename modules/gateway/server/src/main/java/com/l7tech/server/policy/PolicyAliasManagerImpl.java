package com.l7tech.server.policy;

import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.service.AliasManagerImpl;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 12:19:02 PM
 */
public class PolicyAliasManagerImpl extends AliasManagerImpl<PolicyAlias, PolicyHeader>
        implements PolicyAliasManager{

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

