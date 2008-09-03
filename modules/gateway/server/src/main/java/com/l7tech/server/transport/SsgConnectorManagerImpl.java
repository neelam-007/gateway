package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.util.*;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.FirewallRules;
import com.l7tech.server.event.EntityInvalidationEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link SsgConnectorManager}.
 */
public class SsgConnectorManagerImpl
        extends HibernateEntityManager<SsgConnector, EntityHeader>
        implements SsgConnectorManager, InitializingBean, DisposableBean
{
    protected static final Logger logger = Logger.getLogger(SsgConnectorManagerImpl.class.getName());

    public static final String SYSPROP_FIREWALL_RULES_FILENAME = "com.l7tech.server.firewall.rules.filename";
    public static final String SYSPROP_FIREWALL_UPDATE_PROGRAM = "com.l7tech.server.firewall.update.program";
    private static final String DEFAULT_FIREWALL_UPDATE_PROGRAM = "appliance/libexec/update_firewall";
    private static final String FIREWALL_RULES_FILENAME = SyspropUtil.getString(SYSPROP_FIREWALL_RULES_FILENAME,
                                                                                "firewall_rules");

    private final ServerConfig serverConfig;
    @SuppressWarnings( { "FieldCanBeLocal" } )
    private final ApplicationListener applicationListener; // need reference to prevent listener gc
    private final Map<Long, SsgConnector> knownConnectors = new LinkedHashMap<Long, SsgConnector>();
    private final Map<Endpoint, SsgConnector> httpConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());
    private final Map<Endpoint, SsgConnector> httpsConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());
    private final File sudo;

    public SsgConnectorManagerImpl(ServerConfig serverConfig, ApplicationEventProxy eventProxy) {
        this.serverConfig = serverConfig;
        File sudo = null;
        try {
            sudo = SudoUtils.findSudo();
        } catch (IOException e) {
            /* FALLTHROUGH and do without */
        }
        this.sudo = sudo;
        this.applicationListener = new ApplicationListener() {
            public void onApplicationEvent( ApplicationEvent event ) {
                handleEvent(event);
            }
        };

        eventProxy.addApplicationListener( applicationListener );
    }

    public Class<? extends Entity> getImpClass() {
        return SsgConnector.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return SsgConnector.class;
    }

    public String getTableName() {
        return "connector";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CONNECTOR;
    }

    /**
     * Find a connector that uses the specified scheme and provides access to the specified endpoint.
     * <p/>
     * This can be used to provide good default values for the old serverconfig properties
     * httpPort, httpsPort, clusterhttpport, and clusterhttpsport.
     *
     * @param scheme  a scheme, ie SsgConnector.HTTP or SsgConnector.HTTPS.  Required.
     * @param endpoint  an endpoint that must be supported, ie {@link SsgConnector.Endpoint#OTHER_SERVLETS}.
     * @return an appropriate connector, or null if there is no connector with the specified scheme that
     *         provides access to the specified endpoint.
     * @throws com.l7tech.objectmodel.FindException if there is a problem searching for a matching connector
     */
    public SsgConnector find(String scheme, Endpoint endpoint) throws FindException {
        if (scheme == null) throw new NullPointerException("scheme");
        if (endpoint == null) throw new NullPointerException("endpoint");

        Map<Endpoint, SsgConnector> cache = null;
        if (SsgConnector.SCHEME_HTTP.equals(scheme))
            cache = httpConnectorsByService;
        else if (SsgConnector.SCHEME_HTTPS.equals(scheme))
            cache = httpsConnectorsByService;

        {
            SsgConnector connector = cache == null ? null : cache.get(endpoint);
            if (connector != null)
                return connector;
        }

        // Cache miss.  Find a connector with the desired properties.
        // There won't ever be a lot of these so we'll just scan all the ones with the right scheme.
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("enabled", Boolean.TRUE);
        map.put("scheme", scheme);
        List<SsgConnector> list = findMatching(map);
        SsgConnector found = null;
        for (SsgConnector c : list) {
            if (c.offersEndpoint(endpoint) && scheme.equals(c.getScheme())) {
                found = c;
                break;
            }
        }

        if (found != null && cache != null)
            cache.put(endpoint, found);

        return found;
    }

    private void savePublicPort(String varname, String scheme, Endpoint endpoint) throws FindException {
        SsgConnector c = find(scheme, endpoint);
        if (c == null) {
            serverConfig.removeProperty(varname);
        } else {
            serverConfig.putProperty(varname, Integer.toString(c.getPort()));
        }
        logger.info("Default " + scheme + " port: " + serverConfig.getProperty(varname));
    }

    private void updateDefaultPorts() {
        try {
            savePublicPort("defaultHttpPort", SsgConnector.SCHEME_HTTP, Endpoint.OTHER_SERVLETS);
            savePublicPort("defaultHttpsPort", SsgConnector.SCHEME_HTTPS, Endpoint.OTHER_SERVLETS);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to update default HTTP and HTTPS ports for built-in services: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();

        // Initialize known connections
        for (SsgConnector connector : findAll())
            if (connector.isEnabled()) knownConnectors.put(connector.getOid(), connector);

        updateDefaultPorts();

        File program = getFirewallUpdater();
        if (program != null && sudo != null)
            logger.log(Level.INFO, "Using firewall rules updater program: sudo " + program);
        openFirewallForConnectors();
    }

    public void destroy() throws Exception {
        closeFirewallForConnectors();
    }

    private void openFirewallForConnectors() {
        try {
            File conf = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_CONFIG_DIRECTORY, "/ssg/etc/conf", true);
            String firewallRules = new File(conf, FIREWALL_RULES_FILENAME).getPath();

            int rmiPort = serverConfig.getIntProperty(ServerConfig.PARAM_CLUSTER_PORT, 2124);

            FirewallRules.writeFirewallDropfile(firewallRules, rmiPort, knownConnectors.values());

            runFirewallUpdater(firewallRules);

        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to update port list dropfile " + FIREWALL_RULES_FILENAME + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void closeFirewallForConnectors() {
        if (sudo == null)
            return;
        File program = getFirewallUpdater();
        if (program == null)
            return;

        File conf = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_CONFIG_DIRECTORY, "/ssg/etc/conf", true);
        String rulesFile = new File(conf, FIREWALL_RULES_FILENAME).getPath();

        try {
            ProcUtils.exec(null, sudo, new String[] { program.getAbsolutePath(), rulesFile, "stop" }, null, false);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to execute firewall rules program: " + program + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
        }
    }

    /**
     * Passes the specified firewall rules file to the firewall updater program, if one is configured.
     *
     * @param rulesFile the rules file.  Required.
     */
    private void runFirewallUpdater(String rulesFile) {
        if (sudo == null)
            return;
        File program = getFirewallUpdater();
        if (program == null)
            return;

        try {
            ProcUtils.exec(null, sudo, new String[] { program.getAbsolutePath(), rulesFile, "start" }, null, false);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to execute firewall rules program: " + program + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e) );
        }
    }

    /** @return the program to be run whenever the firewall rules change, or null to take no such action. */
    private File getFirewallUpdater() {
        File ssgHome = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_SSG_HOME_DIRECTORY, "/ssg", false);
        File defaultProgram = new File(ssgHome, DEFAULT_FIREWALL_UPDATE_PROGRAM);
        String program = SyspropUtil.getString(SYSPROP_FIREWALL_UPDATE_PROGRAM, defaultProgram.getPath());
        if (program == null || program.length() < 1)
            return null;
        File file = new File(program);
        return file.exists() && file.canExecute() ? file : null;
    }

    private void handleEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent))
            return;
        EntityInvalidationEvent evt = (EntityInvalidationEvent)event;
        if (!SsgConnector.class.isAssignableFrom(evt.getEntityClass()))
            return;
        long[] ids = evt.getEntityIds();
        char[] ops = evt.getEntityOperations();
        for (int i = 0; i < ops.length; i++) {
            char op = ops[i];
            long id = ids[i];

            switch (op) {
                case EntityInvalidationEvent.DELETE:
                    knownConnectors.remove(id);
                    break;
                default:
                    onConnectorChanged(id);
            }
        }
        httpConnectorsByService.clear();
        httpsConnectorsByService.clear();
        updateDefaultPorts();
        openFirewallForConnectors();
    }

    private void onConnectorChanged(long id) {
        try {
            SsgConnector connector = findByPrimaryKey(id);
            if (connector != null && connector.isEnabled())
                knownConnectors.put(id, connector);
            else
                knownConnectors.remove(id);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find just-added or -updated connector with oid " + id + ": " + ExceptionUtils.getMessage(e), e);
        }
    }
}
