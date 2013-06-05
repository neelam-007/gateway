package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyAlias;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.service.AliasManagerImpl;
import com.l7tech.objectmodel.AliasHeader;

/**
 * @author darmstrong
 */
public class PolicyAliasManagerImpl extends AliasManagerImpl<PolicyAlias, Policy, PolicyHeader>
        implements PolicyAliasManager
{
    //- PUBLIC

    @Override
    public Class<PolicyAlias> getImpClass() {
        return PolicyAlias.class;
    }

    @Override
    public Class<PolicyAlias> getInterfaceClass() {
        return PolicyAlias.class;
    }

    @Override
    public String getTableName() {
        return "policy_alias";
    }

    @Override
    public PolicyHeader getNewEntityHeader(PolicyHeader ph) {
        return new PolicyHeader(ph);
    }

    //- PROTECTED

    @Override
    protected AliasHeader<Policy> newHeader( final PolicyAlias entity ) {
        final AliasHeader<Policy> header = new AliasHeader<>(entity);
        header.setSecurityZoneOid(entity.getSecurityZone() == null ? null : entity.getSecurityZone().getOid());
        return header;
    }

}

