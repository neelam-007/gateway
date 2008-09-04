package com.l7tech.server.transport;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.util.*;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.FirewallUtils;
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

    private final ServerConfig serverConfig;
    @SuppressWarnings( { "FieldCanBeLocal" } )
    private final ApplicationListener applicationListener; // need reference to prevent listener gc
    private final Map<Long, SsgConnector> knownConnectors = new LinkedHashMap<Long, SsgConnector>();
    private final Map<Endpoint, SsgConnector> httpConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());
    private final Map<Endpoint, SsgConnector> httpsConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());

    public SsgConnectorManagerImpl(ServerConfig serverConfig, ApplicationEventProxy eventProxy) {
        this.serverConfig = serverConfig;
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

        openFirewallForConnectors();
    }

    public void destroy() throws Exception {
        closeFirewallForConnectors();
    }

    private void openFirewallForConnectors() {
        File conf = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_CONFIG_DIRECTORY, "/opt/SecureSpan/Gateway/Nodes/default/etc/conf", true);
        int rmiPort = serverConfig.getIntProperty(ServerConfig.PARAM_CLUSTER_PORT, 2124);

        List<SsgConnector> connectors = new ArrayList<SsgConnector>(knownConnectors.values());

        // Add a pseudo-connector for the inter-node communication port
        SsgConnector rc = new SsgConnector();
        rc.setPort(rmiPort);
        connectors.add(rc);

        FirewallUtils.openFirewallForConnectors( conf, connectors );
    }

    private void closeFirewallForConnectors() {
        File conf = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_CONFIG_DIRECTORY, "/opt/SecureSpan/Gateway/Nodes/default/etc/conf", true);
        FirewallUtils.closeFirewallForConnectors( conf );
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
