package com.l7tech.server.transport;

import com.l7tech.common.io.PortRange;
import com.l7tech.common.io.PortRanges;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.FirewallUtils;
import com.l7tech.util.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.*;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

/**
 * Implementation of {@link SsgConnectorManager}.
 */
public class SsgConnectorManagerImpl
        extends HibernateEntityManager<SsgConnector, EntityHeader>
        implements SsgConnectorManager, InitializingBean, DisposableBean, PropertyChangeListener, ApplicationContextAware
{
    protected static final Logger logger = Logger.getLogger(SsgConnectorManagerImpl.class.getName());
    private static final String PROP_RESERVED_PORTS = "com.l7tech.server.transport.reservedPorts";

    private final ServerConfig serverConfig;
    private final Map<Goid, SsgConnector> knownConnectors = new LinkedHashMap<Goid, SsgConnector>();
    private final Map<Endpoint, SsgConnector> httpConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());
    private final Map<Endpoint, SsgConnector> httpsConnectorsByService = Collections.synchronizedMap(new HashMap<Endpoint, SsgConnector>());
    private final ConcurrentMap<String, Pair<TransportDescriptor, TransportModule>> modularTransportsByScheme =
            new ConcurrentSkipListMap<String, Pair<TransportDescriptor, TransportModule>>(String.CASE_INSENSITIVE_ORDER);

    private final AtomicReference<Pair<String, Set<InterfaceTag>>> interfaceTags = new AtomicReference<Pair<String, Set<InterfaceTag>>>(null);
    private ApplicationEventPublisher applicationEventPublisher;

    public SsgConnectorManagerImpl(ServerConfig serverConfig, ApplicationEventProxy eventProxy) {
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
        return SsgConnector.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return SsgConnector.class;
    }

    @Override
    public String getTableName() {
        return "connector";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SSG_CONNECTOR;
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
        List<SsgConnector> list = findMatching(Collections.singletonList( map ));
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
        logger.info( "Default " + scheme + " port: " + serverConfig.getProperty( varname ) );
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
    public String translateBindAddress(String bindAddress, int port) throws ListenerException {
        if (!looksLikeInterfaceTagName(bindAddress))
            return bindAddress;

        // Try to look up as interface tag
        Pair<String, Set<InterfaceTag>> info = getInterfaceTagsCached();
        if (info == null)
            throw new ListenerException("No interface definitions exist; unable to find match for listen port " + port + " using interface " + bindAddress);

        List<InetAddress> localAddrs;
        try {
            localAddrs = InetAddressUtil.findAllLocalInetAddresses();
        } catch (SocketException e) {
            throw new ListenerException("Unable to look up network interfaces while finding a match for listen port " +
                               port + " using interface " + bindAddress + ": " + ExceptionUtils.getMessage(e));
        }

        InterfaceTag tag = findTagByName(info.right, bindAddress);
        if (tag == null)
            throw new ListenerException("No interface definition named " + bindAddress + " is known (for listen port " + port + ")");

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
            throw new ListenerException("No address pattern for interface named " + bindAddress + " matches any network address on this node (for listen port " + port + ")");

        return match.getHostAddress();
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public void registerTransportProtocol(TransportDescriptor transportDescriptor, TransportModule transportModule) {
        String scheme = transportDescriptor.getScheme();
        if (scheme == null || scheme.trim().length() < 1)
            throw new IllegalArgumentException("The transportDescriptor must provide a protocol scheme.");
        Pair<TransportDescriptor, TransportModule> prev = modularTransportsByScheme.put(scheme, new Pair<TransportDescriptor, TransportModule>(transportDescriptor, transportModule));
        if (prev != null && prev.left != transportDescriptor) {
            logger.log(Level.WARNING, "More than one attempt to register custom transport protocol " + scheme);
        }
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public TransportModule unregisterTransportProtocol(String protocolName) {
        Pair<TransportDescriptor, TransportModule> prev = modularTransportsByScheme.remove(protocolName);
        return prev != null ? prev.right : null;
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public TransportDescriptor[] getTransportProtocols() {
        List<TransportDescriptor> ret = new ArrayList<TransportDescriptor>();
        Collection<Pair<TransportDescriptor, TransportModule>> pairs = modularTransportsByScheme.values();
        for (Pair<TransportDescriptor, TransportModule> pair : pairs) {
            ret.add(pair.left);
        }
        return ret.toArray(new TransportDescriptor[ret.size()]);
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public PortRanges getReservedPorts() {
        Collection<PortRange> ret = new ArrayList<PortRange>();

        String portsStr = ConfigFactory.getProperty( PROP_RESERVED_PORTS, null );
        if (portsStr != null) {
            String[] ranges = portsStr.split(",");
            for (String range : ranges) {
                String[] begend = range.split("\\-", 2);
                if (begend.length > 0) {
                    try {
                        int low = Integer.parseInt(begend[0]);
                        int high = begend.length < 2 ? low : Integer.parseInt(begend[1]);
                        ret.add(new PortRange(low, high, false));
                    } catch (IllegalArgumentException e) {
                        logger.log(Level.WARNING, "Ignoring invalid range in " + PROP_RESERVED_PORTS);
                    }
                }
            }
        }

        return new PortRanges(ret);
    }

    protected boolean looksLikeInterfaceTagName(String maybeTagname) {
        // It's an interface tag if's non empty and doesn't start with an ASCII digit
        if (maybeTagname == null || maybeTagname.length() < 1 || maybeTagname.contains(":"))
            return false;
        char initial = maybeTagname.charAt(0);
        return (!(initial >= '0' && initial <= '9'));
    }

    private InterfaceTag findTagByName(Collection<InterfaceTag> tags, String desiredName) {
        for (InterfaceTag tag : tags) {
            if (tag.getName().equalsIgnoreCase(desiredName))
                return tag;
        }
        return null;
    }

    private Pair<String, Set<InterfaceTag>> loadInterfaceTags() throws FindException, ParseException {
        String stringForm= ConfigFactory.getUncachedConfig().getProperty( InterfaceTag.PROPERTY_NAME );
        Set<InterfaceTag> tags = stringForm == null ? Collections.<InterfaceTag>emptySet() : InterfaceTag.parseMultiple(stringForm);
        return new Pair<String, Set<InterfaceTag>>(stringForm, tags);
    }

    protected Pair<String, Set<InterfaceTag>> getInterfaceTagsCached() {
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!InterfaceTag.PROPERTY_NAME.equals(evt.getPropertyName()))
            return;

        Pair<String, Set<InterfaceTag>> tags = interfaceTags.get();
        if (tags == null || tags.left == null)
            return;

        interfaceTags.set(null);
        Pair<String, Set<InterfaceTag>> newTags = getInterfaceTagsCached();
        if (((newTags.left == null) != (tags.left == null)) || !newTags.left.equals(tags.left))
            restartAllConnectorsUsingInterfaceTags();
    }

    private void restartAllConnectorsUsingInterfaceTags() {
        if (applicationEventPublisher == null)
            return;

        List<Goid> goids = new ArrayList<Goid>();
        try {
            for (SsgConnector connector : findAll()) {
                if (looksLikeInterfaceTagName(connector.getProperty(SsgConnector.PROP_BIND_ADDRESS)))
                    goids.add(connector.getGoid());
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to update connectors using interface definitions: unable to fetch list of connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getMessage(e));
        }

        EntityInvalidationEvent eie = new EntityInvalidationEvent(this, SsgConnector.class, goids.toArray(new Goid[goids.size()]), ArrayUtils.fill(new char[goids.size()], 'U'));
        applicationEventPublisher.publishEvent(eie);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationEventPublisher = applicationContext;
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();

        // Initialize known connections
        for (SsgConnector connector : findAll())
            if (connector.isEnabled()) knownConnectors.put(connector.getGoid(), connector);

        updateDefaultPorts();

        openFirewallForConnectors();
    }

    @Override
    public void destroy() throws Exception {
        closeFirewallForConnectors();
    }

    private void openFirewallForConnectors() {
        File conf = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        List<SsgConnector> connectors = new ArrayList<SsgConnector>(knownConnectors.values());
        connectors = Functions.map(connectors, new Functions.Unary<SsgConnector, SsgConnector>() {
            @Override
            public SsgConnector call(SsgConnector ssgConnector) {
                return translateSsgConnectorBindAddress(ssgConnector);
            }
        });
        FirewallUtils.openFirewallForConnectors( conf, connectors );
    }

    private SsgConnector translateSsgConnectorBindAddress(SsgConnector ssgConnector) {
        SsgConnector ret = ssgConnector.getCopy();
        try {
            String bindAddress = ssgConnector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
            if (looksLikeInterfaceTagName(bindAddress)) {
                String translated = translateBindAddress(bindAddress, ssgConnector.getPort());
                ret.putProperty(SsgConnector.PROP_BIND_ADDRESS, translated);
            }
        } catch (ListenerException e) {
            logger.log(Level.WARNING, "Unable to translate bind address while updating firewall rules: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return ret;
    }

    private void closeFirewallForConnectors() {
        File conf = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_VAR_DIRECTORY, true);
        FirewallUtils.closeFirewallForConnectors( conf );
    }

    private void handleEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent))
            return;
        EntityInvalidationEvent evt = (EntityInvalidationEvent)event;
        if (!SsgConnector.class.isAssignableFrom(evt.getEntityClass()))
            return;
        Goid[] ids = evt.getEntityIds();
        char[] ops = evt.getEntityOperations();
        for (int i = 0; i < ops.length; i++) {
            char op = ops[i];
            Goid goid = ids[i];

            switch (op) {
                case EntityInvalidationEvent.DELETE:
                    knownConnectors.remove(goid);
                    break;
                default:
                    onConnectorChanged(goid);
            }
        }
        httpConnectorsByService.clear();
        httpsConnectorsByService.clear();
        updateDefaultPorts();
        openFirewallForConnectors();
    }

    private void onConnectorChanged(Goid goid) {
        try {
            SsgConnector connector = findByPrimaryKey(goid);
            if (connector != null && connector.isEnabled())
                knownConnectors.put(goid, connector);
            else
                knownConnectors.remove(goid);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to find just-added or -updated connector with oid " + goid + ": " + ExceptionUtils.getMessage(e), e);
        }
    }
}
