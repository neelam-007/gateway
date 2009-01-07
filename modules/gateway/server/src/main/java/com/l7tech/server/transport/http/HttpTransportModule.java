package com.l7tech.server.transport.http;

import com.l7tech.common.io.IOUtils;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.tomcat.*;
import com.l7tech.server.transport.SsgConnectorActivationListener;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.naming.resources.FileDirContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import javax.naming.directory.DirContext;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that owns the Tomcat servlet container and all the HTTP/HTTPS connectors.
 * <p/>
 * It listens for entity chagne events for SsgConnector to know when to start/stop HTTP connectors.
 */
public class HttpTransportModule extends TransportModule implements PropertyChangeListener {
    protected static final Logger logger = Logger.getLogger(HttpTransportModule.class.getName());

    public static final String RESOURCE_PREFIX = "com/l7tech/server/resources/";
    public static final String CONNECTOR_ATTR_KEYSTORE_PASS = "keystorePass";
    public static final String CONNECTOR_ATTR_TRANSPORT_MODULE_ID = "httpTransportModuleInstanceId";
    public static final String CONNECTOR_ATTR_CONNECTOR_OID = "ssgConnectorOid";
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    private static final AtomicLong nextInstanceId = new AtomicLong(1);
    private static final Map<Long, Reference<HttpTransportModule>> instancesById =
            new ConcurrentHashMap<Long, Reference<HttpTransportModule>>();

    static boolean testMode = false; // when test mode enabled, will always use the following two fields
    static SsgConnector testConnector = null;
    static HttpTransportModule testModule = null;

    private final long instanceId;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ServerConfig serverConfig;
    private final AuditContext auditContext;
    private final MasterPasswordManager masterPasswordManager;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final DefaultKey defaultKeyManager;
    private final Object connectorCrudLuck = new Object();
    private final Map<Long, Pair<SsgConnector, Connector>> activeConnectors = new ConcurrentHashMap<Long, Pair<SsgConnector, Connector>>();
    private final Set<SsgConnectorActivationListener> endpointListeners;
    private Embedded embedded;
    private Engine engine;
    private Host host;
    private StandardContext context;
    private StandardThreadExecutor executor;

    public HttpTransportModule( final ServerConfig serverConfig,
                                final AuditContext context,
                                final MasterPasswordManager masterPasswordManager,
                                final DefaultKey defaultKeyManager,
                                final SsgKeyStoreManager ssgKeyStoreManager,
                                final LicenseManager licenseManager,
                                final SsgConnectorManager ssgConnectorManager,
                                final Set<SsgConnectorActivationListener> endpointListeners )
    {
        super("HTTP Transport Module", logger, GatewayFeatureSets.SERVICE_HTTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager);
        this.serverConfig = serverConfig;
        this.auditContext = context;
        this.masterPasswordManager = masterPasswordManager;
        this.defaultKeyManager = defaultKeyManager;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.instanceId = nextInstanceId.getAndIncrement();
        this.endpointListeners = endpointListeners;
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<HttpTransportModule>(this));
    }

    private void initializeServletEngine() throws LifecycleException {
        if (!initialized.compareAndSet(false, true)) {
            // Already initialized
            return;
        }

        embedded = new Embedded();

        // Create the thread pool
        executor = new StandardThreadExecutor();
        executor.setName("executor");
        executor.setDaemon(true);
        executor.setMaxIdleTime(serverConfig.getIntProperty(ServerConfig.PARAM_IO_HTTP_POOL_MAX_IDLE_TIME, 60000));
        executor.setMaxThreads(serverConfig.getIntProperty(ServerConfig.PARAM_IO_HTTP_POOL_MAX_CONCURRENCY, 200));
        executor.setMinSpareThreads(serverConfig.getIntProperty(ServerConfig.PARAM_IO_HTTP_POOL_MIN_SPARE_THREADS, 25));
        embedded.addExecutor(executor);
        try {
            executor.start();
        } catch (org.apache.catalina.LifecycleException e) {
            final String msg = "Unable to start executor for HTTP/HTTPS connections: " + ExceptionUtils.getMessage(e);
            getApplicationContext().publishEvent(new TransportEvent(this, Component.GW_HTTPRECV, null, Level.WARNING, "Error", msg));
            throw new LifecycleException(msg, e);
        }

        engine = embedded.createEngine();
        engine.setName("ssg");
        engine.setDefaultHost(getListenAddress());
        embedded.addEngine(engine);

        File inf = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_WEB_DIRECTORY, null, false);
        if (inf == null) throw new LifecycleException("No web directory set");
        if (!inf.exists() || !inf.isDirectory()) throw new LifecycleException("No such directory: " + inf.getPath());

        final String s = inf.getAbsolutePath();
        host = embedded.createHost(getListenAddress(), s);
        host.getPipeline().addValve(new ConnectionIdValve(this));
        host.getPipeline().addValve(new ResponseKillerValve());
        engine.addChild(host);

        context = (StandardContext)embedded.createContext("", s);

        String ssgVarPath = serverConfig.getProperty(ServerConfig.PARAM_VAR_DIRECTORY);
        if (ssgVarPath != null)
            context.setWorkDir(ssgVarPath + File.separatorChar + "work");

        // Turn off persistent/distributed session support, since we aren't even using them (Bug #6124)
        context.setManager(new ManagerBase() {
            AtomicInteger rejects = new AtomicInteger(0);

            @Override
            public int getRejectedSessions() {
                return rejects.get();
            }

            @Override
            public void setRejectedSessions(int i) {
                rejects.set(i);
            }

            @Override
            public void load() throws ClassNotFoundException, IOException {
            }

            @Override
            public void unload() throws IOException {
            }
        });

        context.addParameter(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));
        context.setName("");
        context.setResources(createHybridDirContext(inf));

        StandardWrapper dflt = (StandardWrapper)context.createWrapper();
        dflt.setServletClass(DefaultServlet.class.getName());
        dflt.setName("default");
        dflt.setLoadOnStartup(1);
        context.addChild(dflt);
        context.addServletMapping("/", "default");

        host.addChild(context);
    }

    @Override
    public boolean isLicensed() {
        // XXX At the moment, the only way to install a license is using HTTP.
        // Thus the HTTP subsystem must start even if it is nominally not enabled by the license.
        return true;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (executor == null) return; // not yet started

        final String clusterProp = ServerConfig.PARAM_IO_HTTP_POOL_MAX_CONCURRENCY;
        if (evt.getPropertyName().equals(clusterProp)) {
            try {
                final Object v = evt.getNewValue();
                if (v != null) {
                    final int threads = Integer.parseInt(v.toString());
                    logger.info("Changing maximum HTTP/HTTPS concurrency to " + threads);
                    executor.setMaxThreads(threads);
                    executor.stop();
                    executor.start();
                }
            } catch (NumberFormatException nfe) {
                logger.warning("Unable to parse value of cluster property " + clusterProp + ": " + ExceptionUtils.getMessage(nfe));
            } catch (org.apache.catalina.LifecycleException e) {
                final String msg = "Unable to restart executor after changing property " + clusterProp + ": " + ExceptionUtils.getMessage(e);
                logger.log(Level.SEVERE, msg, e);
                getApplicationContext().publishEvent(new TransportEvent(this, Component.GW_HTTPRECV, null, Level.WARNING, "Error", msg));
            }
        }
    }

    /**
     * Build the hybrid virtual/real DirContext that gets WEB-INF virtually from the class path
     * but everything else from the specified inf directory on disk.
     *
     * @param inf  the /ssg/etc/inf directory.  required
     * @return     a new DirContext that will contain a virtual WEB-INF in addition to the real contents of /ssg/etc/inf/ssg
     */
    private DirContext createHybridDirContext(File inf) {
        // Splice the real on-disk ssg/ subdirectory into our virtual filesystem under /ssg
        File ssgFile = new File(inf, "ssg");
        FileDirContext ssgFileContext = new FileDirContext();
        ssgFileContext.setDocBase(ssgFile.getAbsolutePath());
        VirtualDirContext ssgContext = new VirtualDirContext("ssg", ssgFileContext);

        // Set up our virtual WEB-INF subdirectory
        List<VirtualDirEntry> webinfEntries = new ArrayList<VirtualDirEntry>();
        webinfEntries.add(createDirEntryFromClassPathResource("web.xml"));
        File extraDir = new File(inf, "extra");
        if (extraDir.isDirectory())
            addDirEntriesFromDirectory(webinfEntries, extraDir);

        VirtualDirContext webInfContext = new VirtualDirContext("WEB-INF", webinfEntries.toArray(new VirtualDirEntry[webinfEntries.size()]));

        // Splice it all together
        return new VirtualDirContext("VirtualInf", webInfContext, ssgContext);
    }

    /**
     * Create a VirtualDirEntry out of an item in the server resources classpath.
     *
     * @param name the local name, ie "web.xml".  We will try to load com/l7tech/server/resources/web.xml
     * @return a VirtualDirEntry.  never null
     */
    private VirtualDirEntry createDirEntryFromClassPathResource(String name) {
        InputStream is = null;
        try {
            String fullname = HttpTransportModule.RESOURCE_PREFIX + name;
            is = getClass().getClassLoader().getResourceAsStream(fullname);
            if (is == null)
                throw new MissingResourceException("Missing resource: " + fullname, getClass().getName(), fullname);
            return new VirtualDirEntryImpl(name, IOUtils.slurpStream(is));
        } catch (IOException e) {
            throw (MissingResourceException)new MissingResourceException("Error reading resource: " + name, getClass().getName(), name).initCause(e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    /**
     * Check for any files in the specified directory and if any are present, add them to the virtual web-inf.
     * Does not search any subdirectories.
     *
     * @param collector   a list to which new VirtualDirEntryImpl instances will be added for each file (but not subdirectory)
     *                    in the specified directory.  Required.
     * @param directory   the directory to scan for new files to merge into collector.  Required.
     */
    private static void addDirEntriesFromDirectory(List<VirtualDirEntry> collector, File directory) {
        File[] files = directory.listFiles();
        if (files == null || files.length < 1)
            return;
        for (File file : files) {
            try {
                byte[] fileBytes = IOUtils.slurpFile(file);
                collector.add(new VirtualDirEntryImpl(file.getName(), fileBytes));
                logger.info("Noted add-on configuration file: " + file.getName());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to read add-on configuration file (ignoring it): " + file.getAbsolutePath() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    private void startServletEngine() throws LifecycleException {
        if (!running.compareAndSet(false, true)) {
            // Already started
            return;
        }

        // This thread is responsible for attempting to start the server, and for clearing "running" flag if it fails
        boolean itworked = false;
        try {
            initializeServletEngine();

            embedded.start();
            context.start();

            itworked = true;
        } catch (org.apache.catalina.LifecycleException e) {
            String msg = "Unable to start HTTP listener subsystem: " + ExceptionUtils.getMessage(e);
            getApplicationContext().publishEvent(new TransportEvent(this, Component.GW_HTTPRECV, null, Level.WARNING, "Error", msg));
            throw new LifecycleException(e);
        } finally {
            if (!itworked)
                running.set(false);
        }
    }

    private static String getListenAddress() {
        String addr;
        try {
            addr = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            addr = "0.0.0.0";
        }
        return addr;
    }

    /**
     * Stop the running Gateway.
     * This Gateway instance can not be started again once it is stopped and should be destroyed and discarded.
     *
     * @throws LifecycleException if there is a problem shutting down the server
     */
    @Override
    protected void doStop() throws LifecycleException {
        if (!running.get())
            return;
        try {
            embedded.stop();
            running.set(false);
            if (executor != null)
                executor.stop();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        }
    }

    /**
     * Destroy the transport module.
     * This transport module instance can not be started again once it is destroyed and should be discarded.
     *
     * @throws LifecycleException if there is a problem shutting down the server
     */
    @Override
    protected void doClose() throws LifecycleException {
        try {
            stop();
            embedded.destroy();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        } finally {
            instancesById.remove(instanceId);
        }
    }

    private void startInitialConnectors(boolean actuallyStartThem) throws ListenerException {
        final boolean wasSystem = auditContext.isSystem();
        try {
            auditContext.setSystem(true);
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            boolean foundHttp = false;
            for (SsgConnector connector : connectors) {
                if (connector.isEnabled() && connectorIsOwnedByThisModule(connector)) {
                    foundHttp = true;
                    try {
                        if (actuallyStartThem) addConnector(connector);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                    ": " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }

            if (!foundHttp )  {
                createFallbackConnectors(actuallyStartThem);
            }

            createRequiredConnectors(connectors, actuallyStartThem);

        } catch (FindException e) {
            throw new ListenerException("Unable to find initial connectors: " + ExceptionUtils.getMessage(e), e);
        } finally {
            auditContext.setSystem(wasSystem);
        }
    }

    /**
     * Add some connectors to the DB table, getting them from server.xml if possible, but just creating
     * some defaults if not.
     *
     * @param actuallyStartThem if true, start each connector immediately after saving it.
     * @return zero or more connectors that have already been saved to the database.  Never null or empty.
     */
    private Collection<SsgConnector> createFallbackConnectors(boolean actuallyStartThem) {
        Collection<SsgConnector> toAdd = DefaultHttpConnectors.getDefaultConnectors();
        for (SsgConnector connector : toAdd) {
            try {
                ssgConnectorManager.save(connector);
                if (actuallyStartThem) addConnector(connector);
            } catch (SaveException e) {
                logger.log(Level.WARNING, "Unable to save fallback connector to DB: " + ExceptionUtils.getMessage(e), e);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                            ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return toAdd;
    }

    /**
     * Add any required connectors to the DB.
     *
     * @param actuallyStartThem if true, start each connector immediately after saving it.
     * @return zero or more connectors that have already been saved to the database.  Never null or empty.
     */
    private Collection<SsgConnector> createRequiredConnectors(Collection<SsgConnector> currentConnectors, boolean actuallyStartThem) {
        Collection<SsgConnector> toAdd = DefaultHttpConnectors.getRequiredConnectors(currentConnectors);
        for (SsgConnector connector : toAdd) {
            try {
                ssgConnectorManager.save(connector);
                if (actuallyStartThem) addConnector(connector);
            } catch (SaveException e) {
                logger.log(Level.WARNING, "Unable to save required connector to DB: " + ExceptionUtils.getMessage(e), e);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                            ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return toAdd;
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        if ( connector.getOid() == SsgConnector.DEFAULT_OID )
            throw new ListenerException("Connector must be persistent.");
        
        if (isCurrent(connector.getOid(), connector.getVersion()))
            return;

        synchronized (connectorCrudLuck) {
            removeConnector(connector.getOid());
            if (!connectorIsOwnedByThisModule(connector))
                return;

            connector = connector.getReadOnlyCopy();

            Map<String, Object> connectorAttrs = asTomcatConnectorAttrs(connector);
            connectorAttrs.remove("scheme");
            final String scheme = connector.getScheme();
            if (SsgConnector.SCHEME_HTTP.equals(scheme)) {
                addHttpConnector(connector, connector.getPort(), connectorAttrs);
            } else if (SsgConnector.SCHEME_HTTPS.equals(scheme)) {
                addHttpsConnector(connector, connector.getPort(), connectorAttrs);
            } else {
                // It's not an HTTP connector; ignore it.  This shouldn't be possible
                logger.log(Level.WARNING, "HttpTransportModule is ignoring non-HTTP(S) connector with scheme " + scheme);
            }
        }

        notifyEndpointActivation( connector );
    }

    /**
     *
     */
    private boolean isCurrent( long oid, int version ) {
        boolean current;

        synchronized (connectorCrudLuck) {
            Pair<SsgConnector, Connector> entry = activeConnectors.get(oid);
            current = entry != null && entry.left.getVersion()==version;
        }

        return current;
    }

    private Map<String, Object> asTomcatConnectorAttrs(SsgConnector c) throws ListenerException {
        Map<String, Object> m = new LinkedHashMap<String, Object>();

        m.put("maxThreads", "150");
        m.put("minSpareThreads", "25");
        m.put("maxSpareThreads", "75");
        m.put("disableUploadTimeout", "true");
        m.put("acceptCount", "100");

        String bindAddress = c.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if (bindAddress == null || bindAddress.equals("*") || bindAddress.equals("0.0.0.0")) {
            m.remove("address");
        } else {
            m.put("address", bindAddress);
        }

        m.put(CONNECTOR_ATTR_TRANSPORT_MODULE_ID, Long.toString(instanceId));
        m.put(CONNECTOR_ATTR_CONNECTOR_OID, Long.toString(c.getOid()));

        if (SsgConnector.SCHEME_HTTPS.equals(c.getScheme())) {
            // SSL
            m.put("sslProtocol", "TLS");
            m.put("SSLImplementation", "com.l7tech.server.tomcat.SsgSSLImplementation");
            m.put("secure", Boolean.toString(c.isSecure()));

            final int clientAuth = c.getClientAuth();
            if (clientAuth == SsgConnector.CLIENT_AUTH_ALWAYS)
                m.put("clientAuth", "true");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_OPTIONAL)
                m.put("clientAuth", "want");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_NEVER)
                m.put("clientAuth", "false");

            if (c.getKeystoreOid() == null) {
                try {
                    SsgKeyEntry defaultKey = defaultKeyManager.getSslInfo();
                    m.put(SsgJSSESocketFactory.ATTR_KEYALIAS, defaultKey.getAlias());
                    m.put(SsgJSSESocketFactory.ATTR_KEYSTOREOID, String.valueOf(defaultKey.getKeystoreId()));
                } catch (IOException e) {
                    throw new ListenerException("No default SSL key is currently available: " + ExceptionUtils.getMessage(e), e);
                }
            } else {
                m.put(SsgJSSESocketFactory.ATTR_KEYALIAS, c.getKeyAlias());
                m.put(SsgJSSESocketFactory.ATTR_KEYSTOREOID, c.getKeystoreOid().toString());
            }

            String cipherList = c.getProperty(SsgConnector.PROP_CIPHERLIST);
            if (cipherList != null)
                m.put(SsgJSSESocketFactory.ATTR_CIPHERNAMES, cipherList);

        } else {
            // Not SSL
            m.put("connectionTimeout", "20000");
            m.put("socketFactory", "com.l7tech.server.tomcat.SsgServerSocketFactory");
        }

        // Allow overrides of any parameters we didn't already handle
        List<String> extraPropNames = c.getPropertyNames();
        for (String name : extraPropNames) {
            if (name.equals(SsgConnector.PROP_BIND_ADDRESS) ||
                name.equals(SsgConnector.PROP_PORT_RANGE_START) ||
                name.equals(SsgConnector.PROP_PORT_RANGE_COUNT) ||
                name.equals(SsgConnector.PROP_CIPHERLIST))
                continue;
            String value = c.getProperty(name);
            if (value != null) m.put(name, value);
        }

        return m;
    }

    /**
     * Get the last error that occurred while starting a specified connector.
     * <p/>
     * This does not clear the error status for the specified connector.  To clear the error, call
     * clearConnectorError().
     *
     * @param oid  the connector to check.
     * @return  the last error encountered while starting (or stopping) this connector, or null if none recorded.
     */
    public Throwable getConnectorError(long oid) {
        return connectorErrors.get(oid);
    }

    /**
     * Clear the last error status for the specified connector.  If any error status is cleared,
     * it will be returned.
     *
     * @param oid the connector whose error status to clear.
     * @return the error status that was cleared, or null.
     */
    public Throwable clearConnectorError(long oid) {
        return connectorErrors.remove(oid);
    }

    @Override
    protected void removeConnector(long oid) {
        synchronized (connectorCrudLuck) {
            Pair<SsgConnector, Connector> entry = activeConnectors.remove(oid);
            if (entry == null) return;
            Connector connector = entry.right;
            logger.info("Removing " + connector.getScheme() + " connector on port " + connector.getPort());
            embedded.removeConnector(connector);
            closeAllSockets(oid);
            try {
                connector.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception while destroying connector for port " + entry.left.getPort() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    private final Set<String> schemes = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(SsgConnector.SCHEME_HTTP, SsgConnector.SCHEME_HTTPS)));
    @Override
    protected Set<String> getSupportedSchemes() {
        //noinspection ReturnOfCollectionOrArrayField
        return schemes;
    }


    @Override
    public void init() {
        try {
            initializeServletEngine();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doStart() throws LifecycleException {
        if (isStarted())
            return;
        try {
            startServletEngine();
            startInitialConnectors(false); // ensure we can find some initial connectors but don't start them yet (Bug #4500)
        } catch (ListenerException e) {
            throw new LifecycleException("Unable to start HTTP transport module: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (TransportModule.isEventIgnorable(applicationEvent)) {
            return;
        }

        super.onApplicationEvent(applicationEvent);
        if (!isStarted())
            return;
        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors(true);
            } catch (ListenerException e) {
                logger.log(Level.SEVERE, "Unable to start HTTP connectors", e);
            }
        }
    }

    /** @return the Gateway's global ApplicationContext.  Will be present before any servlet contexts are created. */
    @Override
    public ApplicationContext getApplicationContext() {
        return super.getApplicationContext();
    }

    private final Map<Long, Map<Socket, Object>> socketsByConnector = new HashMap<Long, Map<Socket, Object>>();

    private void onSocketOpened(long connectorOid, Socket accepted) {
        synchronized (socketsByConnector) {
            Map<Socket, Object> sockets = socketsByConnector.get(connectorOid);
            if (sockets == null) {
                sockets = new WeakHashMap<Socket, Object>();
                socketsByConnector.put(connectorOid, sockets);
            }
            sockets.put(accepted, Boolean.TRUE);
        }
    }

    private void onSocketClosed(long connectorOid, Socket closed) {
        synchronized (socketsByConnector) {
            Map<Socket, Object> sockets = socketsByConnector.get(connectorOid);
            if (sockets == null)
                return;
            sockets.remove(closed);
            if (sockets.isEmpty())
                socketsByConnector.remove(connectorOid);
        }
    }

    private void closeAllSockets(final long connectorOid) {
        final Map<Socket, Object> sockets;
        synchronized (socketsByConnector) {
            sockets = socketsByConnector.remove(connectorOid);
        }
        if (sockets == null || sockets.isEmpty())
            return;
        final Collection<Socket> ks = new ArrayList<Socket>(sockets.keySet());
        for (Socket socket : ks) {
            if (socket == null)
                continue;
            forceClosed(socket, connectorOid);
        }
    }

    private static void forceClosed(Socket socket, long connectorOid) {
        if (!socket.isClosed()) {
            try {
                logger.log(Level.INFO, "Force closing client connection for connector OID " + connectorOid);
                socket.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error closing client socket: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    /**
     * Report that a client connection has just been opened.
     *
     * @param transportModuleId  the ID of the HttpTransportModule instance that owns the connector.  Required
     * @param connectorOid       the OID of the SsgConnector whose listen socket produced the connection.  Required
     * @param accepted           the just-accepted client connection socket.  Required
     */
    public static void onSocketOpened(long transportModuleId, long connectorOid, Socket accepted) {
        HttpTransportModule module = getInstance(transportModuleId);
        if (module != null) module.onSocketOpened(connectorOid, accepted);
    }

    /**
     * Report that a client connection has just been closed.
     *
     * @param transportModuleId  the ID of the HttpTransportModule instance that owns the connector.  Required
     * @param connectorOid       the OID of the SsgConnector whose listen socket produced the connection.  Required
     * @param closed             the just-closed client connection socket.  Required
     */
    public static void onSocketClosed(long transportModuleId, long connectorOid, Socket closed) {
        HttpTransportModule module = getInstance(transportModuleId);
        if (module != null) module.onSocketClosed(connectorOid, closed);
    }

    private void addHttpConnector(SsgConnector ssgConn, int port, Map<String, Object> attrs) throws ListenerException {
        Connector c = embedded.createConnector((String)null, port, "http");
        c.setEnableLookups(false);
        setConnectorAttributes(c, attrs);

        ProtocolHandler ph = c.getProtocolHandler();
        if (ph instanceof Http11Protocol) {
            ((Http11Protocol)ph).setExecutor(executor);
        } else
            throw new ListenerException("Unable to start HTTP listener on port " + c.getPort() + ": Unrecognized protocol handler: " + ph.getClass().getName());

        activateConnector(ssgConn, c);
    }

    private void addHttpsConnector(SsgConnector ssgConn, int port, Map<String, Object> attrs) throws ListenerException {
        Connector c = embedded.createConnector((String)null, port, "https");
        c.setScheme("https");
        c.setProperty("SSLEnabled","true");
        c.setSecure(true);
        c.setEnableLookups(false);

        // If the connector is trying to add an encrypted password, decrypt it first.
        // If it can't be decrypted, ignore it and fall back to the password from keystore.properties.
        Object kspass = attrs.get(CONNECTOR_ATTR_KEYSTORE_PASS);
        if (kspass != null && masterPasswordManager.looksLikeEncryptedPassword(kspass.toString())) {
            try {
                char[] decrypted = masterPasswordManager.decryptPassword(kspass.toString());
                attrs.put(CONNECTOR_ATTR_KEYSTORE_PASS, new String(decrypted));
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to decrypt encrypted password in server.xml -- falling back to password from keystore.properties: " + ExceptionUtils.getMessage(e));
                attrs.remove(CONNECTOR_ATTR_KEYSTORE_PASS);
            }
        }

        setConnectorAttributes(c, attrs);

        ProtocolHandler ph = c.getProtocolHandler();
        if (ph instanceof Http11Protocol) {
            ((Http11Protocol)ph).setExecutor(executor);
        } else
            throw new ListenerException("Unable to start HTTPS listener on port " + c.getPort() + ": Unrecognized protocol handler: " + ph.getClass().getName());

        activateConnector(ssgConn, c);
    }

    private void activateConnector(SsgConnector connector, Connector c) throws ListenerException {
        embedded.addConnector(c);
        try {
            logger.info("Starting " + c.getScheme() + " connector on port " + c.getPort());
            c.start();
            activeConnectors.put(connector.getOid(), new Pair<SsgConnector, Connector>(connector, c));
        } catch (org.apache.catalina.LifecycleException e) {
            embedded.removeConnector(c);
            final String msg = "Unable to start " + c.getScheme() + " listener on port " + c.getPort() + ": " + ExceptionUtils.getMessage(e);
            getApplicationContext().publishEvent(new TransportEvent(this, Component.GW_HTTPRECV, null, Level.WARNING, "Error", msg));
            throw new ListenerException(msg, e);
        }
    }

    private static void setConnectorAttributes(Connector c, Map<String, Object> attrs) {
        for (Map.Entry<String, Object> entry : attrs.entrySet())
            c.setAttribute(entry.getKey(), entry.getValue());
    }

    Engine getEngine() {
        return engine;
    }

    Host getHost() {
        return host;
    }

    /**
     * Get the unique identifier for this HttpTransportModule instance.
     * <p/>
     * This is used because there is no way to pass non-String arguments as connector attributes,
     * and since the socket factories are instantiated by Tomcat using reflection to invoke a nullary constructor,
     * there is no other way to pass a reference to ourself through to our socket factories.
     *
     * @see #getInstance(long id)
     * @return the unique identifier for this HttpTransportModule instance within this classloader.
     */
    public long getInstanceId() {
        return instanceId;
    }

    /**
     * Find the SsgConnector instance whose listener accepted the specified HttpServletRequest,
     * if we have enough information to make that determination.
     *
     * @param req the request to identify.  Required.
     * @return the SsgConnector instance whose listener accepted this request, or null if it can't be found.
     */
    public static SsgConnector getConnector(HttpServletRequest req) {
        if (testMode) return testConnector;

        Long connectorOid = (Long)req.getAttribute(ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID);
        if (connectorOid == null) {
            logger.log(Level.WARNING, "Request lacks valid attribute " + ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID);
            return null;
        }

        Long htmId = (Long)req.getAttribute(ConnectionIdValve.ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID);
        HttpTransportModule htm = getInstance(htmId);
        if (htm == null) {
            logger.log(Level.WARNING, "Request lacks valid attribute " + ConnectionIdValve.ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID);
            return null;
        }

        Pair<SsgConnector, Connector> pair = htm.activeConnectors.get(connectorOid);
        if (pair == null) {
            logger.log(Level.WARNING, "Request lacks valid attribute " + ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID +
                                      ": No active connector with oid " + connectorOid);
            return null;
        }
        return pair.left;
    }

    /**
     * Assert that the specified servlet request arrived over a connector that is configured to grant access
     * to the specified endpoint.
     * <p/>
     * If this method returns, an SsgConnector instance was located, and it granted access to the specified endpoint.
     *
     * @param req  the request to examine.  Required.
     * @param endpoint  the endpoint to require.  Required.
     * @throws ListenerException if no SsgConnector could be found for this request, or a connector was found but
     *                           was not configured to grant access to the specified endpoint.
     */
    public static void requireEndpoint(HttpServletRequest req, SsgConnector.Endpoint endpoint) throws ListenerException {
        SsgConnector connector = getConnector(req);
        if (connector == null)
            throw new ListenerException("No connector was found for the specified request.");
        if (!connector.offersEndpoint(endpoint))
            throw new ListenerException("This request cannot be accepted on this port.");
    }

    /**
     * Get the SsgKeyStoreManager instance made available for SSL connectors created by this transport module.
     *
     * @return an SsgKeyStoreManager instance.  Should never be null.
     */
    public SsgKeyStoreManager getSsgKeyStoreManager() {
        return ssgKeyStoreManager;
    }

    /**
     * Find the HttpTransportModule corresponding to the specified instance ID.
     * <p/>
     * This is normally used by the SsgJSSESocketFactory to locate a Connector's owner HttpTransportModule
     * so it can get at the SsgKeyStoreManager.
     *
     * @see #getInstanceId
     * @param id the instance ID to search for.  Required.
     * @return  the corresopnding HttpTransportModule instance, or null if not found.
     */
    public static HttpTransportModule getInstance(long id) {
        if (testMode) return testModule;
        
        Reference<HttpTransportModule> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }

    /**
     * Dispatch notifications for endpoint activation. 
     */
    private void notifyEndpointActivation( final SsgConnector connector ) {      
        for ( SsgConnectorActivationListener listener : endpointListeners ) {
            try {
                listener.notifyActivated( connector );
            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Unexpected error during connector activation notification.", e );    
            }
        }
    }
}
