package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.policy.variable.Syntax;

/**
 * Created with IntelliJ IDEA.
 * User: kdiep
 * Date: 2/12/13
 * Time: 3:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class FirewallSelector implements ExpandVariables.Selector<SsgFirewallRule> {

    @Override
    public Selection select(final String contextName, final SsgFirewallRule firewallRule, final String name, final Syntax.SyntaxErrorHandler handler, final boolean strict) {
        if(name.equals("port")){
            return new Selection( firewallRule.getPort());
        } else if(name.equals("name")){
            return new Selection( firewallRule.getName());
        } else if(name.equals("interfaces")){
            String bindAddress = firewallRule.getProperty("bindAddress");
            return new Selection(bindAddress == null ? "(ALL)" : bindAddress);
        } else if(name.equals("enabled")){
            return new Selection( firewallRule.isEnabled()?"Yes":"No");
        } else if(name.equals("protocol")){
            return new Selection( firewallRule.getProtocol());
        } else if(name.equals("ordinal")){
            return new Selection( firewallRule.getOrdinal());
        }

        return null;
    }

    @Override
    public Class<SsgFirewallRule> getContextObjectClass() {
        return SsgFirewallRule.class;
    }
}
