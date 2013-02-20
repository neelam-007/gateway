package com.l7tech.server.transport.firewall;

import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.FirewallUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>An implementation of the {@link SsgFirewallRuleManager}.</p>
 * @author K.Diep
 */
public class SsgFirewallRuleManagerImpl extends HibernateEntityManager<SsgFirewallRule, EntityHeader>
        implements SsgFirewallRuleManager, DisposableBean {

    private static final Logger logger = Logger.getLogger(SsgFirewallRuleManagerImpl.class.getName());

    private final ServerConfig serverConfig;
    private final Map<Long, SsgFirewallRule> knownFirewallRules = new LinkedHashMap<Long, SsgFirewallRule>();

    public SsgFirewallRuleManagerImpl(ServerConfig serverConfig, ApplicationEventProxy eventProxy) {
        this.serverConfig = serverConfig;

        eventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                handleEvent(event);
            }
        });
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SsgFirewallRule.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return SsgFirewallRule.class;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.FIREWALL_RULE;
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();
        for(SsgFirewallRule rule : findAll()){
            knownFirewallRules.put(rule.getOid(), rule);
        }
        openFirewallForRule();
    }

    @Override
    public void destroy() throws Exception {
        File conf = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        FirewallUtils.closeFirewallForConnectors(conf);
    }

    private void handleEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent))
            return;
        EntityInvalidationEvent evt = (EntityInvalidationEvent)event;
        if (!SsgFirewallRule.class.isAssignableFrom(evt.getEntityClass()))
            return;
        long[] ids = evt.getEntityIds();
        char[] ops = evt.getEntityOperations();
        for (int i = 0; i < ops.length; i++) {
            char op = ops[i];
            long id = ids[i];

            switch (op) {
                case EntityInvalidationEvent.DELETE:
                    logger.warning("deleting firewall rule");
                    knownFirewallRules.remove(id);
                    break;
                default:
                    onFirewallRuleChanged(id);
            }
        }
        openFirewallForRule();
    }

    private void onFirewallRuleChanged(long id) {
        try {
            SsgFirewallRule rule = findByPrimaryKey(id);
            if (rule != null && rule.isEnabled())
                knownFirewallRules.put(id, rule);
            else
                knownFirewallRules.remove(id);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find just-added or -updated firewall with oid " + id + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void openFirewallForRule() {
        File conf = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        List<SsgFirewallRule> rules = Functions.sort(knownFirewallRules.values(), new Comparator<SsgFirewallRule>() {
            @Override
            public int compare(SsgFirewallRule a, SsgFirewallRule b) {
                return Integer.compare(a.getOrdinal(), b.getOrdinal());
            }
        });

        FirewallUtils.openFirewallForRules( conf, rules );
    }
}


