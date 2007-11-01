package com.l7tech.server.transport.http;

import com.l7tech.common.LicenseManager;
import com.l7tech.common.security.MasterPasswordManager;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.Pair;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.tomcat.*;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Embedded;
import org.apache.naming.resources.FileDirContext;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.coyote.ProtocolHandler;
import org.springframework.context.ApplicationContext;

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
    static SsgConnector testConnector;
    static HttpTransportModule testModule;

    private final long instanceId;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ServerConfig serverConfig;
    private final MasterPasswordManager masterPasswordManager;
    private final KeystoreUtils keystoreUtils;
    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final Map<Long, Pair<SsgConnector, Connector>> activeConnectors = new ConcurrentHashMap<Long, Pair<SsgConnector, Connector>>();
    private Embedded embedded;
    private Engine engine;
    private Host host;
    private StandardContext context;
    private StandardThreadExecutor executor;

    public HttpTransportModule(ServerConfig serverConfig,
                               MasterPasswordManager masterPasswordManager,
                               KeystoreUtils keystoreUtils,
                               SsgKeyStoreManager ssgKeyStoreManager,
                               LicenseManager licenseManager,
                               SsgConnectorManager ssgConnectorManager)
    {
        super("HTTP Transport Module", logger, GatewayFeatureSets.SERVICE_HTTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager);
        this.serverConfig = serverConfig;
        this.masterPasswordManager = masterPasswordManager;
        this.keystoreUtils = keystoreUtils;
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.instanceId = nextInstanceId.getAndIncrement();
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
            throw new LifecycleException("Unable to start executor for HTTP/HTTPS connections: " + ExceptionUtils.getMessage(e), e);
        }

        engine = embedded.createEngine();
        engine.setName("ssg");
        engine.setDefaultHost(getListenAddress());
        embedded.addEngine(engine);

        File ssgHome = serverConfig.getLocalDirectoryProperty(ServerConfig.PARAM_SSG_HOME_DIRECTORY, null, false);
        if (ssgHome == null) throw new LifecycleException("No ssgHome set");
        File inf = new File(ssgHome, "etc" + File.separator + "inf");
        if (!inf.exists() || !inf.isDirectory()) throw new LifecycleException("No such directory: " + inf.getPath());

        final String s = inf.getAbsolutePath();
        host = embedded.createHost(getListenAddress(), s);
        host.getPipeline().addValve(new ConnectionIdValve(this));
        host.getPipeline().addValve(new ResponseKillerValve());
        engine.addChild(host);

        context = (StandardContext)embedded.createContext("", s);
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
                logger.log(Level.SEVERE, "Unable to restart executor after changing property " + clusterProp + ": " + ExceptionUtils.getMessage(e), e);
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

        // Set up our virtual WEB-INF subdirectory and virtual favicon and index.html
        List<VirtualDirEntry> webinfEntries = new ArrayList<VirtualDirEntry>();
        webinfEntries.add(createDirEntryFromClassPathResource("web.xml"));
        File extraDir = new File(inf, "extra");
        if (extraDir.isDirectory())
            addDirEntriesFromDirectory(webinfEntries, extraDir);

        VirtualDirContext webInfContext = new VirtualDirContext("WEB-INF", webinfEntries.toArray(new VirtualDirEntry[0]));
        VirtualDirEntry favicon = createDirEntryFromClassPathResource("favicon.ico");
        VirtualDirEntry indexhtml = createDirEntryFromClassPathResource("index.html");

        // Splice it all together
        return new VirtualDirContext("VirtualInf", webInfContext, favicon, indexhtml, ssgContext);
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
            return new VirtualDirEntryImpl(name, HexUtils.slurpStream(is));
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
    private void addDirEntriesFromDirectory(List<VirtualDirEntry> collector, File directory) {
        File[] files = directory.listFiles();
        if (files == null || files.length < 1)
            return;
        for (File file : files) {
            try {
                byte[] fileBytes = HexUtils.slurpFile(file);
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
            throw new LifecycleException(e);
        } finally {
            if (!itworked)
                running.set(false);
        }
    }

    private String getListenAddress() {
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
    protected void doStop() throws LifecycleException {
        if (!running.get())
            return;
        try {
            embedded.stop();
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

    private void startInitialConnectors() throws ListenerException {
        try {
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            if (connectors.isEmpty()) {
                logger.warning("No connectors defined in database.  Will attempt to import from server.xml");
                createFallbackConnectors();
                connectors = ssgConnectorManager.findAll();
            }
            for (SsgConnector connector : connectors) {
                if (connector.isEnabled() && connectorIsOwnedByThisModule(connector)) {
                    try {
                        addConnector(connector);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                    ": " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }

        } catch (FindException e) {
            throw new ListenerException("Unable to find initial connectors: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Add some connectors to the DB table, getting them from server.xml if possible, but just creating
     * some defaults if not.
     */
    private void createFallbackConnectors() {
        Collection<SsgConnector> toAdd = DefaultHttpConnectors.makeFallbackConnectors(serverConfig);
        for (SsgConnector connector : toAdd) {
            try {
                ssgConnectorManager.save(connector);
            } catch (SaveException e) {
                logger.log(Level.WARNING, "Unable to save fallback connector to DB: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    protected synchronized void addConnector(SsgConnector connector) throws ListenerException {
        removeConnector(connector.getOid());
        if (!connectorIsOwnedByThisModule(connector))
            return;

        Map<String, Object> connectorAttrs = asTomcatConnectorAttrs(connector);
        connectorAttrs.remove("scheme");
        final String scheme = connector.getScheme();
        if (SsgConnector.SCHEME_HTTP.equals(scheme)) {
            final Connector c = addHttpConnector(connector.getPort(), connectorAttrs);
            activeConnectors.put(connector.getOid(), new Pair<SsgConnector, Connector>(connector, c));
        } else if (SsgConnector.SCHEME_HTTPS.equals(scheme)) {
            final Connector c = addHttpsConnector(connector.getPort(), connectorAttrs);
            activeConnectors.put(connector.getOid(), new Pair<SsgConnector, Connector>(connector, c));
        } else {
            // It's not an HTTP connector; ignore it.  This shouldn't be possible
            logger.log(Level.WARNING, "HttpTransportModule is ignoring non-HTTP(S) connector with scheme " + scheme);
        }
    }

    private Map<String, Object> asTomcatConnectorAttrs(SsgConnector c) {
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
                m.put("keystoreFile", keystoreUtils.getSslKeystorePath());
                m.put("keystorePass", keystoreUtils.getSslKeystorePasswd());
                m.put("keystoreType", keystoreUtils.getSslKeyStoreType());
                m.put("keyAlias", keystoreUtils.getSSLAlias());
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
            m.put(name, value);
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

    protected synchronized void removeConnector(long oid) {
        Pair<SsgConnector, Connector> entry = activeConnectors.remove(oid);
        if (entry == null) return;
        Connector connector = entry.right;
        logger.info("Removing " + connector.getScheme() + " connector on port " + connector.getPort());
        embedded.removeConnector(connector);
        closeAllSockets(oid);
    }

    private final Set<String> schemes = new HashSet<String>(Arrays.asList(SsgConnector.SCHEME_HTTP, SsgConnector.SCHEME_HTTPS));
    protected Set<String> getSupportedSchemes() {
        return schemes;
    }


    public void init() {
        try {
            initializeServletEngine();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doStart() throws LifecycleException {
        if (isStarted())
            return;
        try {
            startServletEngine();
            startInitialConnectors();
        } catch (ListenerException e) {
            throw new LifecycleException("Unable to start HTTP transport module: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /** @return the Gateway's global ApplicationContext.  Will be present before any servlet contexts are created. */
    public ApplicationContext getApplicationContext() {
        return super.getApplicationContext();
    }

    private final Map<Long, Map<Socket, Object>> socketsByConnector = new HashMap<Long, Map<Socket, Object>>();

    private synchronized void onSocketOpened(long connectorOid, Socket accepted) {
        Map<Socket, Object> sockets = socketsByConnector.get(connectorOid);
        if (sockets == null) {
            sockets = new WeakHashMap<Socket, Object>();
            socketsByConnector.put(connectorOid, sockets);
        }
        sockets.put(accepted, Boolean.TRUE);
    }

    private synchronized void onSocketClosed(long connectorOid, Socket closed) {
        Map<Socket, Object> sockets = socketsByConnector.get(connectorOid);
        if (sockets == null)
            return;
        sockets.remove(closed);
        if (sockets.isEmpty())
            socketsByConnector.remove(connectorOid);
    }

    private void closeAllSockets(long connectorOid) {
        Map<Socket, Object> sockets = socketsByConnector.get(connectorOid);
        if (sockets == null || sockets.isEmpty())
            return;
        Collection<Socket> ks = new ArrayList<Socket>(sockets.keySet());
        synchronized (this) {
            for (Socket socket : ks) {
                if (socket != null) {
                    try {
                        if (!socket.isClosed()) {
                            logger.log(Level.INFO, "Force closing client connection for connector OID " + connectorOid);
                            socket.close();
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error closing client socket: " + ExceptionUtils.getMessage(e), e);
                    }
                }
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

    public Connector addHttpConnector(int port, Map<String, Object> attrs) throws ListenerException {
        Connector c = embedded.createConnector((String)null, port, "http");
        c.setEnableLookups(false);
        setConnectorAttributes(c, attrs);

        ProtocolHandler ph = c.getProtocolHandler();
        if (ph instanceof Http11Protocol) {
            ((Http11Protocol)ph).setExecutor(executor);
        } else
            throw new ListenerException("Unable to start HTTP listener on port " + c.getPort() + ": Unrecognized protocol handler: " + ph.getClass().getName());

        embedded.addConnector(c);
        try {
            logger.info("Starting HTTP connector on port " + port);
            c.start();
            return c;
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTP listener: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public Connector addHttpsConnector(int port,
                                  Map<String, Object> attrs)
            throws ListenerException
    {
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

        embedded.addConnector(c);
        try {
            logger.info("Starting HTTPS connector on port " + port);
            c.start();
            return c;
        } catch (org.apache.catalina.LifecycleException e) {
            throw new ListenerException("Unable to start HTTPS listener: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void setConnectorAttributes(Connector c, Map<String, Object> attrs) {
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
}
