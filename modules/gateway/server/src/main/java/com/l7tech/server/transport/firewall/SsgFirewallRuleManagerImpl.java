package com.l7tech.server.transport.firewall;

import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.FirewallUtils;
import com.l7tech.util.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>An implementation of the {@link SsgFirewallRuleManager}.</p>
 * @author K.Diep
 */
public class SsgFirewallRuleManagerImpl extends HibernateGoidEntityManager<SsgFirewallRule, EntityHeader>
        implements SsgFirewallRuleManager, DisposableBean {

    private static final Logger logger = Logger.getLogger(SsgFirewallRuleManagerImpl.class.getName());

    private final ServerConfig serverConfig;
    private final Map<Goid, SsgFirewallRule> knownFirewallRules = new LinkedHashMap<Goid, SsgFirewallRule>();
    private final AtomicReference<Pair<String, Set<InterfaceTag>>> interfaceTags = new AtomicReference<Pair<String, Set<InterfaceTag>>>(null);

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
            knownFirewallRules.put(rule.getGoid(), rule);
        }
        openFirewallForRule();
    }

    @Override
    public void destroy() throws Exception {
        File conf = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        FirewallUtils.closeFirewallForConnectors(conf);
    }

    private void handleEvent(ApplicationEvent event) {
        if (!(event instanceof GoidEntityInvalidationEvent))
            return;
        GoidEntityInvalidationEvent evt = (GoidEntityInvalidationEvent)event;
        if (!SsgFirewallRule.class.isAssignableFrom(evt.getEntityClass()))
            return;
        Goid[] ids = evt.getEntityIds();
        char[] ops = evt.getEntityOperations();
        for (int i = 0; i < ops.length; i++) {
            char op = ops[i];
            Goid goid = ids[i];

            switch (op) {
                case GoidEntityInvalidationEvent.DELETE:
                    logger.warning("deleting firewall rule");
                    knownFirewallRules.remove(goid);
                    break;
                default:
                    onFirewallRuleChanged(goid);
            }
        }
        openFirewallForRule();
    }

    private void onFirewallRuleChanged(Goid goid) {
        try {
            SsgFirewallRule rule = findByPrimaryKey(goid);
            if (rule != null && rule.isEnabled())
                knownFirewallRules.put(goid, rule);
            else
                knownFirewallRules.remove(goid);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find just-added or -updated firewall with goid " + goid + ": " + ExceptionUtils.getMessage(e), e);
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
        rules = Functions.map(rules, new Functions.Unary<SsgFirewallRule, SsgFirewallRule>() {
            @Override
            public SsgFirewallRule call(SsgFirewallRule rule) {
                rule = translateBindAddressForRule(rule, "in-interface");
                rule = translateBindAddressForRule(rule, "out-interface");
                return rule;
            }
        });
        FirewallUtils.openFirewallForRules( conf, rules );
    }

    private SsgFirewallRule translateBindAddressForRule(SsgFirewallRule rule, String source) {
        try {
            String bindAddress = rule.getProperty(source);
            if (looksLikeInterfaceTagName(bindAddress)) {
                String translated = translateBindAddress(bindAddress);
                rule.putProperty(source, translated);
            }
        } catch (ListenerException e) {
            logger.log(Level.WARNING, "Unable to translate bind address while updating firewall rules: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return rule;
    }

    private boolean looksLikeInterfaceTagName(String maybeTagname) {
        // It's an interface tag if's non empty and doesn't start with an ASCII digit
        if (maybeTagname == null || maybeTagname.length() < 1 || maybeTagname.contains(":"))
            return false;
        char initial = maybeTagname.charAt(0);
        return (!(initial >= '0' && initial <= '9'));
    }

    private String translateBindAddress(String bindAddress) throws ListenerException {
        if (!looksLikeInterfaceTagName(bindAddress))
            return bindAddress;

        // Try to look up as interface tag
        Pair<String, Set<InterfaceTag>> info = getInterfaceTagsCached();
        if (info == null)
            throw new ListenerException("No interface definitions exist; unable to find match for firewall rule using interface " + bindAddress);

        List<InetAddress> localAddrs;
        try {
            localAddrs = InetAddressUtil.findAllLocalInetAddresses();
        } catch (SocketException e) {
            throw new ListenerException("Unable to look up network interfaces while finding a match for firewall rule using interface " + bindAddress + ": " + ExceptionUtils.getMessage(e));
        }

        InterfaceTag tag = findTagByName(info.right, bindAddress);
        if (tag == null)
            throw new ListenerException("No interface definition named " + bindAddress + " is known.");

        Set<String> patterns = tag.getIpPatterns();
        InetAddress match = null;
        outer: for (InetAddress addr : localAddrs) {
            for (String pattern : patterns) {
                if (InetAddressUtil.patternMatchesAddress(pattern, addr)) {
                    if (match == null) {
                        match = addr;
                    } else {
                        logger.log(Level.WARNING, "Interface " + bindAddress + " contains patterns matching more than one network addresses on this node.  Will use first match of " + match);
                        break outer;
                    }
                }
            }
        }

        if (match == null)
            throw new ListenerException("No address pattern for interface named " + bindAddress + " matches any network address on this node for firewall rule.");

        return match.getHostAddress();
    }

    private Pair<String, Set<InterfaceTag>> getInterfaceTagsCached() {
        Pair<String, Set<InterfaceTag>> tagInfo = interfaceTags.get();
        if (tagInfo == null) {
            try {
                tagInfo = loadInterfaceTags();
                interfaceTags.set(tagInfo);
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to load interface definitions: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Invalid interface definition: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
        return tagInfo;
    }

    private Pair<String, Set<InterfaceTag>> loadInterfaceTags() throws FindException, ParseException {
        String stringForm= ConfigFactory.getUncachedConfig().getProperty( InterfaceTag.PROPERTY_NAME );
        Set<InterfaceTag> tags = stringForm == null ? Collections.<InterfaceTag>emptySet() : InterfaceTag.parseMultiple(stringForm);
        return new Pair<String, Set<InterfaceTag>>(stringForm, tags);
    }

    private InterfaceTag findTagByName(Collection<InterfaceTag> tags, String desiredName) {
        for (InterfaceTag tag : tags) {
            if (tag.getName().equalsIgnoreCase(desiredName))
                return tag;
        }
        return null;
    }
}


