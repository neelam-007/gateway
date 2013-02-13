package com.l7tech.server.transport.firewall;

import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.EntityManagerStub;

/**
 * Created with IntelliJ IDEA.
 * User: kdiep
 * Date: 2/12/13
 * Time: 1:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SsgFirewallRulesManagerStub extends EntityManagerStub<SsgFirewallRule, EntityHeader> implements SsgFirewallRulesManager{

    public SsgFirewallRulesManagerStub(final SsgFirewallRule ... entitiesIn){
        super(entitiesIn);
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SsgFirewallRule.class;
    }

}
