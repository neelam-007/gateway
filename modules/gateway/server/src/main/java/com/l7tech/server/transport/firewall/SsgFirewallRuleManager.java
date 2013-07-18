package com.l7tech.server.transport.firewall;

import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.GoidEntityManager;

/**
 * <p>An entity manager to manage {@link SsgFirewallRule}.</p>
 * @author K.Diep
 */
public interface SsgFirewallRuleManager extends GoidEntityManager<SsgFirewallRule, EntityHeader> {
}
