package com.l7tech.server.transport.firewall;

import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.EntityManagerStub;

/**
 * <p>
 *     A test implementation to {@link SsgFirewallRuleManager}.
 * </p>
 * @author K.Diep
 */
public class SsgFirewallRulesManagerStub extends EntityManagerStub<SsgFirewallRule, EntityHeader> implements SsgFirewallRuleManager {

    public SsgFirewallRulesManagerStub(final SsgFirewallRule ... entitiesIn){
        super(entitiesIn);
    }

    @Override
    public Class<? extends SsgFirewallRule> getImpClass() {
        return SsgFirewallRule.class;
    }

}
