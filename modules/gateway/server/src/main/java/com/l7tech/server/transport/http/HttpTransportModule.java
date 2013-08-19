package com.l7tech.server.transport.http;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.tomcat.*;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorActivationListener;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.util.*;
import com.l7tech.util.Functions.UnaryVoid;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.naming.resources.FileDirContext;
import org.apache.tomcat.util.IntrospectionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import javax.naming.directory.DirContext;
import javax.servlet.ServletRequest;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.Component.GW_HTTPRECV;
import static com.l7tech.gateway.common.transport.SsgConnector.Endpoint.*;
import static com.l7tech.gateway.common.transport.SsgConnector.SCHEME_HTTP;
import static com.l7tech.gateway.common.transport.SsgConnector.SCHEME_HTTPS;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_HTTP_MESSAGE_INPUT;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ValidationUtils.isValidInteger;

/**
 * Bean that owns the Tomcat servlet container and all the HTTP/HTTPS connectors.
 * <p/>
 * It listens for entity change events for SsgConnector to know when to start/stop HTTP connectors.
 */
public class HttpTransportModule extends TransportModule implements PropertyChangeListener {
    protected static final Logger logger = Logger.getLogger(HttpTransportModule.class.getName());

    public static final String RESOURCE_PREFIX = "com/l7tech/server/resources/";
    public static final String CONNECTOR_ATTR_KEYSTORE_PASS = "keystorePass";
    public static final String CONNECTOR_ATTR_TRANSPORT_MODULE_ID = "httpTransportModuleInstanceId";
    public static final String CONNECTOR_ATTR_CONNECTOR_OID = "ssgConnectorOid";
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    private static final AtomicLong nextInstanceId = new AtomicLong(1L);
    private static final Map<Long, Reference<HttpTransportModule>> instancesById =
            new ConcurrentHashMap<Long, Reference<HttpTransportModule>>();

    static boolean testMode = false; // when test mode enabled, will always use the following two fields
    static SsgConnector testConnector = null;
    static HttpTransportModule testModule = null;

    private final long instanceId;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ServerConfig serverConfig;
    private final MasterPasswordManager masterPasswordManager;
    private final Map<Goid, Pair<SsgConnector, Connector>> activeConnectors = new ConcurrentHashMap<Goid, Pair<SsgConnector, Connector>>();
    private final Set<SsgConnectorActivationListener> endpointListeners;

    private Embedded embedded;
    private StandardContext context;
    private StandardThreadExecutor executor;

    public HttpTransportModule( final ServerConfig serverConfig,
                                final MasterPasswordManager masterPasswordManager,
                                final DefaultKey defaultKey,
                                final LicenseManager licenseManager,
                                final SsgConnectorManager ssgConnectorManager,
                                final TrustedCertServices trustedCertServices,
                                final Set<SsgConnectorActivationListener> endpointListeners )
    {
        super("HTTP Transport Module", GW_HTTPRECV, logger, SERVICE_HTTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig);
        this.serverConfig = serverConfig;
        this.masterPasswordManager = masterPasswordManager;
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

        String httpSessionName = serverConfig.getProperty( ServerConfigParams.PARAM_HTTP_SESSION_NAME );
        SyspropUtil.setProperty( "org.apache.catalina.SESSION_PARAMETER_NAME", httpSessionName );
        SyspropUtil.setProperty( "org.apache.catalina.SESSION_COOKIE_NAME", httpSessionName );

        // Create the thread pool
        final int maxSize = serverConfig.getIntProperty( ServerConfigParams.PARAM_IO_HTTP_POOL_MAX_CONCURRENCY, 200);
        final int coreSize = serverConfig.getIntProperty( ServerConfigParams.PARAM_IO_HTTP_POOL_MIN_SPARE_THREADS, maxSize / 2);
        executor = createExecutor( "executor", coreSize, maxSize );
        embedded.addExecutor(executor);
        try {
            executor.start();
        } catch (org.apache.catalina.LifecycleException e) {
            final String msg = "Unable to start executor for HTTP/HTTPS connections: " + ExceptionUtils.getMessage(e);
            auditError( "HTTP(S)", msg, getDebugException( e ) );
            throw new LifecycleException(msg, e);
        }

        Engine engine = embedded.createEngine();
        engine.setName("ssg");
        engine.setDefaultHost(InetAddressUtil.getLocalHostName());
        embedded.addEngine(engine);

        File inf = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_WEB_DIRECTORY, null, false);
        if (inf == null) throw new LifecycleException("No web directory set");
        if (!inf.exists() || !inf.isDirectory()) throw new LifecycleException("No such directory: " + inf.getPath());

        final String s = inf.getAbsolutePath();
        Host host = embedded.createHost(InetAddressUtil.getLocalHostName(), s);
        host.getPipeline().addValve(new ConnectionIdValve(this));
        host.getPipeline().addValve(new ResponseKillerValve());
        engine.addChild(host);

        context = (StandardContext)embedded.createContext("", s);

        String ssgVarPath = serverConfig.getProperty( ServerConfigParams.PARAM_VAR_DIRECTORY );
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
        context.addMimeMapping("gif", "image/gif");
        context.addMimeMapping("png", "image/png");
        context.addMimeMapping("jpg", "image/jpeg");
        context.addMimeMapping("jpeg", "image/jpeg");
        context.addMimeMapping("htm", "text/html");
        context.addMimeMapping("html", "text/html");
        context.addMimeMapping("xml", "text/xml");
        context.addMimeMapping("txt", "text/plain");
        context.addMimeMapping("css", "text/css");

        StandardWrapper dflt = (StandardWrapper)context.createWrapper();
        dflt.setServletClass(DefaultServlet.class.getName());
        dflt.setName("default");
        dflt.setLoadOnStartup(1);
        context.addChild(dflt);
        context.addServletMapping("/", "default");

        context.setParentClassLoader(getClass().getClassLoader());
        WebappLoader webappLoader = new WebappLoader(context.getParentClassLoader());
        webappLoader.setDelegate(context.getDelegate());
        webappLoader.setLoaderClass(WebappClassLoaderEx.class.getName());
        context.setLoader(webappLoader);

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

        final String propertyName = evt.getPropertyName();
        if ( ServerConfigParams.PARAM_IO_HTTP_POOL_MAX_CONCURRENCY.equals(propertyName) ||
             ServerConfigParams.PARAM_IO_HTTP_POOL_MIN_SPARE_THREADS.equals(propertyName))
        {
            try {
                int maxSize = serverConfig.getIntProperty( ServerConfigParams.PARAM_IO_HTTP_POOL_MAX_CONCURRENCY, 200);
                int coreSize = serverConfig.getIntProperty( ServerConfigParams.PARAM_IO_HTTP_POOL_MIN_SPARE_THREADS, maxSize / 2);
                Pair<Integer, Integer> adjusted = adjustCoreAndMaxThreads(coreSize, maxSize);
                coreSize = adjusted.left;
                maxSize = adjusted.right;

                logger.info("Changing HTTP/HTTPS concurrency to core=" + coreSize + ", max=" + maxSize);
                if ( maxSize > executor.getMaxThreads() ) {
                    executor.setMaxThreads(maxSize);
                    executor.setMinSpareThreads(coreSize);
                } else {
                    executor.setMinSpareThreads(coreSize);
                    executor.setMaxThreads(maxSize);
                }
                executor.stop();
                executor.start();
            } catch (org.apache.catalina.LifecycleException e) {
                final String msg = "Unable to restart executor after changing property " + evt.getPropertyName() + ": " + ExceptionUtils.getMessage(e);
                logger.log(Level.SEVERE, msg, e);
                getApplicationContext().publishEvent(new TransportEvent(this, GW_HTTPRECV, null, Level.WARNING, "Error", msg));
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
            MissingResourceException mre = new MissingResourceException("Error reading resource: " + name, getClass().getName(), name);
            mre.initCause(e);
            throw mre;
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

    private Pair<Integer, Integer> adjustCoreAndMaxThreads(int coreSize, int maxSize) {
        if ( maxSize < 1 || maxSize > 100000 ) maxSize = 40;
        if ( coreSize < 0 && coreSize > -1000 ) coreSize = maxSize / Math.abs(coreSize);
        if ( coreSize < 1 )
            coreSize = 1;
        else if ( coreSize > maxSize )
            coreSize = maxSize;
        return new Pair<Integer, Integer>(coreSize, maxSize);
    }

    private StandardThreadExecutor createExecutor( final String name,
                                                   int coreSize,
                                                   int maxSize)
    {
        Pair<Integer, Integer> adjusted = adjustCoreAndMaxThreads(coreSize, maxSize);
        coreSize = adjusted.left;
        maxSize = adjusted.right;

        StandardThreadExecutor executor = new StandardThreadExecutor();
        executor.setName(name);
        executor.setDaemon(true);
        executor.setMaxIdleTime( serverConfig.getIntProperty( ServerConfigParams.PARAM_IO_HTTP_POOL_MAX_IDLE_TIME, 60000) );
        executor.setMaxThreads(maxSize);
        executor.setMinSpareThreads(coreSize);
        executor.setNamePrefix("tomcat-exec-" + name + "-");
        return executor;
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
            auditError( "HTTP(S)", msg, getDebugException( e ) );
            throw new LifecycleException(e);
        } finally {
            if (!itworked)
                running.set(false);
        }
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
            final List<Goid> oidsToStop = new ArrayList<Goid>(activeConnectors.keySet());
            for ( final Goid goid : oidsToStop) {
                removeConnector(goid);
            }
        }
        catch(Exception e) {
            auditError( "HTTP(S)", "Error while shutting down.", e);
        }

        try {
            embedded.stop();
            running.set(false);
            unregisterProtocols();
            for ( org.apache.catalina.Executor embeddedExecutor : embedded.findExecutors() ) {
                if ( embeddedExecutor != null )
                    embeddedExecutor.stop();                
            }
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
        final boolean wasSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            boolean foundHttp = false;
            for (SsgConnector connector : connectors) {
                if (connector.isEnabled() && connectorIsOwnedByThisModule(connector)) {
                    foundHttp = true;
                    try {
                        if (actuallyStartThem) addConnector(connector);
                    } catch ( Exception e ) {
                        final Exception auditException;
                        if ( e instanceof ListenerException || ExceptionUtils.getMessage(e).contains("java.net.BindException: ") ) { // The exception cause is not chained ...
                            auditException = ExceptionUtils.getDebugException(e);
                        } else {
                            auditException = e;
                        }
                        auditError( "HTTP(S)", "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                    ": " + ExceptionUtils.getMessage(e), auditException );
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
            AuditContextUtils.setSystem(wasSystem);
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
            } catch (ListenerException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                            ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
     * @param currentConnectors connectors currently in the database
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
            } catch (ListenerException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                            ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                            ": " + ExceptionUtils.getMessage(e), e);
            }
        }
        return toAdd;
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        if ( connector.getGoid().equals(SsgConnector.DEFAULT_GOID ))
            throw new ListenerException("Connector must be persistent.");
        
        if (isCurrent(connector.getGoid(), connector.getVersion()))
            return;

        removeConnector(connector.getGoid());
        if (!connectorIsOwnedByThisModule(connector))
            return;

        connector = connector.getReadOnlyCopy();
        Map<String, Object> connectorAttrs = asTomcatConnectorAttrs(connector);
        connectorAttrs.remove("scheme");
        final String scheme = connector.getScheme();
        if ( SCHEME_HTTP.equals(scheme)) {
            addHttpConnector(connector, connector.getPort(), connectorAttrs);
        } else if ( SCHEME_HTTPS.equals(scheme)) {
            addHttpsConnector(connector, connector.getPort(), connectorAttrs);
        } else {
            // It's not an HTTP connector; ignore it.  This shouldn't be possible
            logger.log(Level.WARNING, "HttpTransportModule is ignoring non-HTTP(S) connector with scheme " + scheme);
        }

        notifyEndpointActivation( connector );
    }

    @Override
    protected boolean isCurrent( Goid goid, int version ) {
        boolean current;

        Pair<SsgConnector, Connector> entry = activeConnectors.get(goid);
        current = entry != null && entry.left.getVersion()==version;

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
        if (bindAddress == null || InetAddressUtil.isAnyHostAddress(bindAddress)) {
            m.remove("address");
        } else {
            m.put("address", ssgConnectorManager.translateBindAddress(bindAddress, c.getPort()));
        }

        m.put(CONNECTOR_ATTR_TRANSPORT_MODULE_ID, Long.toString(instanceId));
        m.put(CONNECTOR_ATTR_CONNECTOR_OID, c.getGoid().toString());

        if ( SCHEME_HTTPS.equals(c.getScheme())) {
            // SSL
            m.put("SSLImplementation", "com.l7tech.server.tomcat.SsgSSLImplementation");
            m.put("secure", Boolean.toString(c.isSecure()));

            final int clientAuth = c.getClientAuth();
            if (clientAuth == SsgConnector.CLIENT_AUTH_ALWAYS)
                m.put("clientAuth", "true");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_OPTIONAL)
                m.put("clientAuth", "want");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_NEVER)
                m.put("clientAuth", "false");

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
                name.equals(SsgConnector.PROP_TLS_CIPHERLIST) ||
                name.equals(SsgConnector.PROP_TLS_PROTOCOLS))
                continue;
            String value = c.getProperty(name);
            if (value != null) m.put(name, value);
        }

        return m;
    }

    @Override
    protected void removeConnector(Goid goid) {
        final Pair<SsgConnector, Connector> entry;
        entry = activeConnectors.remove(goid);
        if (entry == null) return;
        Connector connector = entry.right;
        auditStop( entry.left.getScheme(), describe(entry.left) );
        embedded.removeConnector(connector);
        try {
            connector.destroy();
        } catch (Exception e) {
            auditError( entry.left.getScheme(), "Exception while destroying connector for port " + entry.left.getPort() + ": " + ExceptionUtils.getMessage(e), e );
        }

        org.apache.catalina.Executor connectorExecutor = embedded.getExecutor( executorName(entry.left) );
        if ( connectorExecutor != null ) {
            embedded.removeExecutor( connectorExecutor );
            try {
                connectorExecutor.stop();
            } catch (Exception e) {
                auditError( entry.left.getScheme(), "Exception while destroying thread pool for port " + entry.left.getPort() + ": " + ExceptionUtils.getMessage(e), e );
            }
        }

        notifyEndpointDeactivation(entry.left);
    }

    private final Set<String> schemes = caseInsensitiveSet( SCHEME_HTTP, SCHEME_HTTPS );
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
            registerProtocols();
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
            ((Http11Protocol)ph).setExecutor( createAndRegisterExecutorIfRequired( executorName(ssgConn), ssgConn ) );
        } else
            throw new ListenerException("Unable to start HTTPS listener on port " + c.getPort() + ": Unrecognized protocol handler: " + ph.getClass().getName());

        activateConnector(ssgConn, c);
    }

    /**
     * Create a name for an executor associated with this connector.
     *
     * We'll use this name as a prefix for threads also, so make sure it is unique.
     * @param connector  the connector to base the name on.  Required.
     * @return a name to use for an executor for this connector.  Never null.
     */
    private String executorName( final SsgConnector connector ) {
        StringBuilder builder = new StringBuilder();
        builder.append("connector-");
        for ( char character : connector.getName().toLowerCase().toCharArray() ) {
            if ( (character >= '0' && character <= '9') ||
                 (character >= 'a' && character <= 'z') ) {
                builder.append(character);                
            }
        }
        builder.append("-");
        builder.append(connector.getGoid());
        builder.append("-executor");

        return builder.toString();
    }

    private Executor createAndRegisterExecutorIfRequired( final String name, final SsgConnector connector ) throws ListenerException {
        StandardThreadExecutor connectorExecutor = executor;

        final String sizeStr = connector.getProperty( SsgConnector.PROP_THREAD_POOL_SIZE );
        if ( sizeStr != null ) {
            try {
                final int size = Integer.parseInt(sizeStr);
                connectorExecutor = createExecutor( name, size, size );
                connectorExecutor.start();
                embedded.addExecutor( connectorExecutor );
            } catch ( NumberFormatException nfe ) {
                logger.warning("Ignoring invalid thread pool size '"+sizeStr+"' for connector '"+connector.getName()+"'.");
            } catch (org.apache.catalina.LifecycleException e) {
                throw new ListenerException( "Unable to start HTTPS listener on port " + connector.getPort() + ": Unable to start thread pool '" + ExceptionUtils.getMessage(e) + "'.");
            }
        }

        return connectorExecutor;
    }

    private void registerProtocols() {
        final TransportDescriptor http = new TransportDescriptor("HTTP", false);
        http.setHttpBased(true);
        http.setSupportsHardwiredServiceResolution(true);
        http.setSupportsSpecifiedContentType(true);
        http.setSupportedEndpoints(EnumSet.of(MESSAGE_INPUT, POLICYDISCO, PING, STS, WSDLPROXY, SNMPQUERY, NODE_COMMUNICATION)); // Other two built-in endpoints (CSRHANDLER and PASSWD) are not available for HTTP protocol.
        ssgConnectorManager.registerTransportProtocol(http, this);

        final TransportDescriptor https = new TransportDescriptor("HTTPS", true);
        https.setHttpBased(true);
        https.setSupportsHardwiredServiceResolution(true);
        https.setSupportsSpecifiedContentType(true);
        https.setSupportedEndpoints(EnumSet.allOf(SsgConnector.Endpoint.class));
        ssgConnectorManager.registerTransportProtocol(https, this);
    }

    private void unregisterProtocols() {
        ssgConnectorManager.unregisterTransportProtocol( SCHEME_HTTP );
        ssgConnectorManager.unregisterTransportProtocol( SCHEME_HTTPS );
    }

    private void activateConnector(SsgConnector connector, Connector c) throws ListenerException {
        activeConnectors.put(connector.getGoid(), new Pair<SsgConnector, Connector>(connector, c));
        embedded.addConnector(c);
        try {
            auditStart( connector.getScheme(), describe( connector ) );
            c.start();
        } catch (org.apache.catalina.LifecycleException e) {
            embedded.removeConnector(c);
            final String msg = "Unable to start " + describe( connector ) + ": " + ExceptionUtils.getMessage(e);
            auditError( connector.getScheme(), msg, getDebugException(e) );
            throw new ListenerException(msg, e);
        }
    }

    private static void setConnectorAttributes( final Connector c, final Map<String, Object> attrs ) {
        for ( final Map.Entry<String, Object> entry : attrs.entrySet() ) {
            if ("enableLookups".equalsIgnoreCase(entry.getKey())) {
                c.setEnableLookups(Boolean.valueOf(String.valueOf(entry.getValue())));
            } else if ("maxParameterCount".equalsIgnoreCase(entry.getKey())) {
                callbackIfValidInteger( entry.getValue(), new UnaryVoid<Integer>() {
                    @Override
                    public void call( final Integer value ) {
                        c.setMaxParameterCount(value);
                    }
                } );
            } else if ("maxPostSize".equalsIgnoreCase(entry.getKey())) {
                callbackIfValidInteger( entry.getValue(), new UnaryVoid<Integer>() {
                    @Override
                    public void call( final Integer value ) {
                        c.setMaxPostSize(value);
                    }
                } );
            } else if ("trimContentType".equalsIgnoreCase(entry.getKey())) {
                c.setTrimContentType(Boolean.valueOf(String.valueOf(entry.getValue())));
            } else {
                c.setAttribute(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void callbackIfValidInteger( final Object value, final UnaryVoid<Integer> callback ) {
        final String stringValue = String.valueOf( value ).trim();

        if ( isValidInteger( stringValue, false, Integer.MIN_VALUE, Integer.MAX_VALUE ) ) {
            callback.call( Integer.parseInt( stringValue ) );
        }
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
    public static SsgConnector getConnector( ServletRequest req) {
        if (testMode) return testConnector;

        Goid connectorGoid = (Goid)req.getAttribute(ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID);
        if (connectorGoid == null) {
            logger.log(Level.WARNING, "Request lacks valid attribute " + ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID);
            return null;
        }

        Long htmId = (Long)req.getAttribute(ConnectionIdValve.ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID);
        HttpTransportModule htm = getInstance(htmId);
        if (htm == null) {
            logger.log(Level.WARNING, "Request lacks valid attribute " + ConnectionIdValve.ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID);
            return null;
        }

        Pair<SsgConnector, Connector> pair = htm.activeConnectors.get(connectorGoid);
        if (pair == null) {
            logger.log(Level.WARNING, "Request lacks valid attribute " + ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID +
                                      ": No active connector with oid " + connectorGoid);
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
    public static void requireEndpoint(ServletRequest req, SsgConnector.Endpoint endpoint) throws ListenerException {
        SsgConnector connector = getConnector(req);
        if (connector == null)
            throw new ListenerException("No connector was found for the specified request.");
        if (!connector.offersEndpoint(endpoint))
            throw new ListenerException("This request cannot be accepted on this port.");
    }

    /**
     * Look up an active SsgConnector instance by its GOID.
     * <p/>
     * This can be used by transport drivers instantiated by reflection by third-party code to
     * find their way back to their owning SsgConnector instance.
     *
     * @param goid the GOID of the SsgConnector to locate.
     * @return the specified SsgConnector instance.   Never null.
     * @throws ListenerException if there is no active connector with this OID.
     */
    public SsgConnector getActiveConnectorByGoid(Goid goid) throws ListenerException {
        Pair<SsgConnector, Connector> got = activeConnectors.get( goid );
        if (got == null)
            throw new ListenerException("No active connector exists with oid " + goid);
        return got.left;
    }

    /**
     * Find the HttpTransportModule corresponding to the specified instance ID.
     * <p/>
     * This is normally used by the SsgJSSESocketFactory to locate a Connector's owner HttpTransportModule
     * so it can look up its SsgConnector.
     *
     * @see #getInstanceId
     * @param id the instance ID to search for.  Required.
     * @return  the corresponding HttpTransportModule instance, or null if not found.
     */
    public static HttpTransportModule getInstance(long id) {
        if (testMode) return testModule;
        
        Reference<HttpTransportModule> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }

    /**
     * Dispatch notifications for endpoint activation.
     * @param connector the connector that was just activated. required.
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

    /**
     * Dispatch notifications for endpoint deactivation.
     * @param connector the connector that was just deactivated. required.
     */
    private void notifyEndpointDeactivation( final SsgConnector connector ) {
        for ( SsgConnectorActivationListener listener : endpointListeners ) {
            try {
                listener.notifyDeactivated( connector );
            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Unexpected error during connector deactivation notification.", e );
            }
        }
    }

    /**
     * Provide access to serverConfig for modules.
     *
     * @return the serverConfig
     */
    @Override
    public Config getServerConfig() {
        return serverConfig;
    }

    @Override
    public void reportMisconfiguredConnector(Goid connectorGoid) {
        Pair<SsgConnector, Connector> got = activeConnectors.get( connectorGoid );
        String desc = got == null ? null : got.left == null ? null : " (port " + got.left.getPort() + ")";
        logger.log(Level.WARNING, "Shutting down HTTP connector OID " + connectorGoid + desc + " because it cannot be opened with its current configuration");
        removeConnector(connectorGoid);
    }

    public static final class WebappClassLoaderEx extends WebappClassLoader {
        public WebappClassLoaderEx() {
        }

        public WebappClassLoaderEx( final ClassLoader parent ) {
            super( parent );
        }

        /**
         * Overridden to prevent cleanup that is not necessary in our environment.
         */
        @Override
        protected void clearReferences() {
            IntrospectionUtils.clear();
            org.apache.juli.logging.LogFactory.release(this);
            java.beans.Introspector.flushCaches();
        }
    }
}
