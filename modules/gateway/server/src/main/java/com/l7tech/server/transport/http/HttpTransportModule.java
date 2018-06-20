package com.l7tech.server.transport.http;

import com.google.common.annotations.VisibleForTesting;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayURLStreamHandlerFactory;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.service.BundleBootstrapCompleteEvent;
import com.l7tech.server.tomcat.*;
import com.l7tech.server.transport.*;
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
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.naming.resources.DirContextURLStreamHandlerFactory;
import org.apache.naming.resources.FileDirContext;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jetbrains.annotations.Nullable;
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
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.Component.GW_HTTPRECV;
import static com.l7tech.gateway.common.transport.SsgConnector.Endpoint.*;
import static com.l7tech.gateway.common.transport.SsgConnector.*;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_HTTP_MESSAGE_INPUT;
import static com.l7tech.util.BuildInfo.getProductVersionMajor;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static com.l7tech.util.ConfigFactory.getProperty;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ValidationUtils.isValidInteger;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Bean that owns the Tomcat servlet container and all the HTTP/HTTPS connectors.
 * <p/>
 * It listens for entity change events for SsgConnector to know when to start/stop HTTP connectors.
 */
public class HttpTransportModule extends TransportModule implements PropertyChangeListener {

    private static final Logger LOGGER = Logger.getLogger(HttpTransportModule.class.getName());

    public static final String CONNECTOR_ATTR_TRANSPORT_MODULE_ID = "httpTransportModuleInstanceId";
    public static final String CONNECTOR_ATTR_CONNECTOR_OID = "ssgConnectorOid";
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    /** By default, set to "CA API Gateway for security reasons - overrides Tomcat's Apache-Coyote/1.1 */
    static final String PROP_NAME_SERVER = "server";

    /** This is the default value of the "server" header sent in all responses from a Gateway listen port response,
     * overriding Tomcat's default "Apache-Coyote/1.1" */
    static final String HEADER_SERVER_CONFIG_PROP_DEFAULT = "com.l7tech.server.response.header.server";

    static final String HEADER_SERVER_DEFAULT_FORMAT = "CA-API-Gateway/%s.0";
    private static final String RESOURCE_PREFIX = "com/l7tech/server/resources/";
    private static final String CONNECTOR_ATTR_KEYSTORE_PASS = "keystorePass";
    private static final String CONFIG_PROP_CONCURRENCY_WARNING_REPEAT_DELAY_SEC = "io.httpConcurrencyWarning.repeatDelay";
    private static final String UNABLE_TO_START_CONNECTOR = "Unable to start %s connector on port %s: %s";
    private static final String REQUEST_LACKS_ATTRIBUTE = "Request lacks valid attribute %s";
    private static final String CLIENT_AUTH = "clientAuth";
    private static final String HTTP_S = "HTTP(S)";
    private static final AtomicLong nextInstanceId = new AtomicLong(1L);
    private static final Map<Long, Reference<HttpTransportModule>> instancesById =
            new ConcurrentHashMap<>();

    static boolean testMode = false; // when test mode enabled, will always use the following two fields
    static final String APACHE_ALLOW_BACKSLASH = "org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH";
    static SsgConnector testConnector = null;
    static HttpTransportModule testModule = null;

    private final long instanceId;

    private final Set<String> schemes = caseInsensitiveSet( SCHEME_HTTP, SCHEME_HTTPS );
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger sharedThreadPoolConcurrency = new AtomicInteger();

    private final ServerConfig serverConfig;
    private final MasterPasswordManager masterPasswordManager;
    private final Map<Goid, Pair<SsgConnector, Connector>> activeConnectors = new ConcurrentHashMap<>();
    private final Map<Goid, AtomicInteger> connectorPoolConcurrency = new ConcurrentHashMap<>();
    private final Map<Goid, Integer> connectorConcurrencyWarningThreshold = new ConcurrentHashMap<>();
    private final AtomicLong lastConcurrencyWarningTime = new AtomicLong();

    private Embedded embedded;
    private StandardContext context;
    private StandardThreadExecutor executor;

    public HttpTransportModule( final ServerConfig serverConfig,
                                final MasterPasswordManager masterPasswordManager,
                                final DefaultKey defaultKey,
                                final LicenseManager licenseManager,
                                final SsgConnectorManager ssgConnectorManager,
                                final TrustedCertServices trustedCertServices )
    {
        super("HTTP Transport Module", GW_HTTPRECV, LOGGER, SERVICE_HTTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig);
        this.serverConfig = serverConfig;
        this.masterPasswordManager = masterPasswordManager;
        this.instanceId = nextInstanceId.getAndIncrement();
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<>(this));
    }

    private void initializeServletEngine() throws LifecycleException {
        if (!initialized.compareAndSet(false, true)) {
            // Already initialized
            return;
        }
        // This is originally done by WebappLoader class, but it fails because GatewayURLStreamHandlerFactory is taking place before
        // So we do it the way we support to preserve tomcat behaviour
        GatewayURLStreamHandlerFactory.registerHandlerFactory("jndi", new DirContextURLStreamHandlerFactory());

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
            auditError( HTTP_S, msg, getDebugException( e ) );
            throw new LifecycleException(msg, e);
        }

        Engine engine = embedded.createEngine();
        engine.setName("ssg");
        engine.setDefaultHost(InetAddressUtil.getLocalHostName());
        embedded.addEngine(engine);

        File inf = serverConfig.getLocalDirectoryProperty( ServerConfigParams.PARAM_WEB_DIRECTORY, null, false);
        if (inf == null) {
            throw new LifecycleException("No web directory set");
        }
        if (!inf.exists() || !inf.isDirectory()) {
            throw new LifecycleException("No such directory: " + inf.getPath());
        }

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
            public void load() {
                //
            }

            @Override
            public void unload() {
                //
            }
        });

        boolean enableAdmin = getApplicationContext().containsBean( "adminSubsystemPresent" );

        context.addParameter(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));
        context.setName("");
        context.setResources( createHybridDirContext( inf, enableAdmin ) );
        context.addMimeMapping("gif", "image/gif");
        context.addMimeMapping("png", "image/png");
        context.addMimeMapping("jpg", "image/jpeg");
        context.addMimeMapping("jpeg", "image/jpeg");
        context.addMimeMapping("htm", "text/html");
        context.addMimeMapping("html", "text/html");
        context.addMimeMapping("xml", "text/xml");
        context.addMimeMapping("txt", "text/plain");
        context.addMimeMapping("css", "text/css");
        context.addMimeMapping("jnlp", "application/x-java-jnlp-file");

        StandardWrapper dflt = (StandardWrapper)context.createWrapper();
        dflt.setServletClass(SsgDefaultServlet.class.getName());
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
        if (executor == null) {
            return; // not yet started
        }

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

                LOGGER.info("Changing HTTP/HTTPS concurrency to core=" + coreSize + ", max=" + maxSize);
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
                LOGGER.log(Level.SEVERE, msg, e);
                getApplicationContext().publishEvent(new TransportEvent(this, GW_HTTPRECV, null, Level.WARNING, "Error", msg));
            }
        }
    }

    /**
     * Build the hybrid virtual/real DirContext that gets WEB-INF virtually from the class path
     * but everything else from the specified inf directory on disk.
     *
     * @param inf  the /ssg/etc/inf directory.  required
     * @param enableAdmin if the admin servlets should be instantiated.
     * @return     a new DirContext that will contain a virtual WEB-INF in addition to the real contents of /ssg/etc/inf/ssg
     */
    private DirContext createHybridDirContext( File inf, boolean enableAdmin ) {
        // Splice the real on-disk ssg/ subdirectory into our virtual filesystem under /ssg
        File ssgFile = new File(inf, "ssg");
        FileDirContext ssgFileContext = new FileDirContext();
        ssgFileContext.setDocBase(ssgFile.getAbsolutePath());
        VirtualDirContext ssgContext = new VirtualDirContext("ssg", ssgFileContext);

        // Set up our virtual WEB-INF subdirectory
        List<VirtualDirEntry> webinfEntries = new ArrayList<>();

        String extraAdmin = enableAdmin ? new String( loadResourceBytes( "webxml-admin.dat" ) ) : "<!-- admin subsystem omitted -->";
        String wacRepl = enableAdmin ? "webApplicationContext" : "webApplicationContext-noadmin";

        // TODO replace with lambda when Spring updated
        UnaryOperator<byte[]> webXmlTransformer = new UnaryOperator<byte[]>() {
            @Override
            public byte[] apply( byte[] bytes ) {
                return new String( bytes )
                        .replaceAll( "webApplicationContext", wacRepl )
                        .replaceAll( "\\<includeAdminOnlyContent/\\>", extraAdmin )
                        .getBytes();
            }
        };
        webinfEntries.add(createDirEntryFromClassPathResource("web.xml", webXmlTransformer ) );
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
     * @param fileTransformer an operator to modify the file bytes, or null
     *
     * @return a VirtualDirEntry.  never null
     */
    private VirtualDirEntry createDirEntryFromClassPathResource( String name, @Nullable UnaryOperator<byte[]> fileTransformer ) {
        if ( null == fileTransformer ) {
            // Use identity transform
            // TODO replace with lambda when Spring updated
            fileTransformer = new UnaryOperator<byte[]>() {
                @Override
                public byte[] apply( byte[] bytes ) {
                    return bytes;
                }
            };
        }
        return new VirtualDirEntryImpl(name, fileTransformer.apply( loadResourceBytes( name ) ) );
    }

    private byte[] loadResourceBytes( String name ) {
        String fullname = HttpTransportModule.RESOURCE_PREFIX + name;
        try ( InputStream is = getClass().getClassLoader().getResourceAsStream( fullname ) ) {
            if ( is == null )
                throw new MissingResourceException( "Missing resource: " + fullname, getClass().getName(), fullname );
            return IOUtils.slurpStream( is );
        } catch ( IOException e ) {
            MissingResourceException mre = new MissingResourceException("Error reading resource: " + name, getClass().getName(), name);
            mre.initCause(e);
            throw mre;
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
                LOGGER.info("Noted add-on configuration file: " + file.getName());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Unable to read add-on configuration file (ignoring it): " + file.getAbsolutePath() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    private Pair<Integer, Integer> adjustCoreAndMaxThreads(int coreSize, int maxSize) {
        if ( maxSize < 1 || maxSize > 100000 ) {
            maxSize = 40;
        }
        if ( coreSize < 0 && coreSize > -1000 ) {
            coreSize = maxSize / Math.abs(coreSize);
        }
        if ( coreSize < 1 ) {
            coreSize = 1;
        } else if ( coreSize > maxSize ) {
            coreSize = maxSize;
        }
        return new Pair<>(coreSize, maxSize);
    }

    private StandardThreadExecutor createExecutor( final String name,
                                                   int coreSize,
                                                   int maxSize)
    {
        Pair<Integer, Integer> adjusted = adjustCoreAndMaxThreads(coreSize, maxSize);
        coreSize = adjusted.left;
        maxSize = adjusted.right;

        StandardThreadExecutor threadExecutor = new StandardThreadExecutor();
        threadExecutor.setName(name);
        threadExecutor.setDaemon(true);
        threadExecutor.setMaxIdleTime( serverConfig.getIntProperty( ServerConfigParams.PARAM_IO_HTTP_POOL_MAX_IDLE_TIME, 60000) );
        threadExecutor.setMaxThreads(maxSize);
        threadExecutor.setMinSpareThreads(coreSize);
        threadExecutor.setNamePrefix("tomcat-exec-" + name + "-");
        return threadExecutor;
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
            auditError( HTTP_S, msg, getDebugException( e ) );
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
            final List<Goid> oidsToStop = new ArrayList<>(activeConnectors.keySet());
            for ( final Goid goid : oidsToStop) {
                removeConnector(goid);
            }
        }
        catch(Exception e) {
            auditError( HTTP_S, "Error while shutting down.", e);
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
        boolean allowBackslash = serverConfig.getBooleanProperty( ServerConfigParams.PARAM_IO_HTTP_ALLOW_BACKSLASH, false);
        if (allowBackslash) {
            SyspropUtil.setProperty(APACHE_ALLOW_BACKSLASH, String.valueOf(allowBackslash));
        }
        try {
            AuditContextUtils.setSystem(true);
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            boolean foundHttp = false;
            for (SsgConnector connector : connectors) {
                if (connector.isEnabled() && connectorIsOwnedByThisModule(connector)) {
                    foundHttp = true;
                    this.startConnectorIfNecessary(actuallyStartThem, connector);
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

    private void startConnectorIfNecessary(boolean actuallyStartThem, SsgConnector connector) {
        if (!actuallyStartThem) {
            return;
        }

        try {
            addConnector(connector);
        } catch ( ListenerException e ) {
            auditError( HTTP_S, String.format(UNABLE_TO_START_CONNECTOR, connector.getScheme(), connector.getPort(), ExceptionUtils.getMessage(e)), ExceptionUtils.getDebugException(e) );
        } catch ( Exception e ) {
            final Exception auditException;
            if ( ExceptionUtils.getMessage(e).contains("java.net.BindException: ") ) { // The exception cause is not chained ...
                auditException = ExceptionUtils.getDebugException(e);
            } else {
                auditException = e;
            }
            auditError( HTTP_S, String.format(UNABLE_TO_START_CONNECTOR, connector.getScheme(), connector.getPort(), ExceptionUtils.getMessage(e)), auditException );
        }
    }

    /**
     * Add some connectors to the DB table, getting them from server.xml if possible, but just creating
     * some defaults if not.
     *
     * @param actuallyStartThem if true, start each connector immediately after saving it.
     */
    private void createFallbackConnectors(boolean actuallyStartThem) {
        Collection<SsgConnector> toAdd = DefaultHttpConnectors.getDefaultConnectors();
        for (SsgConnector connector : toAdd) {
            try {
                ssgConnectorManager.save(connector);
                if (actuallyStartThem) {
                    addConnector(connector);
                }
            } catch (SaveException e) {
                LOGGER.log(Level.WARNING, "Unable to save fallback connector to DB: " + ExceptionUtils.getMessage(e), e);
            } catch (ListenerException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                LOGGER.log(Level.WARNING, String.format(UNABLE_TO_START_CONNECTOR, connector.getScheme(), connector.getPort(), ExceptionUtils.getMessage(e)), ExceptionUtils.getDebugException(e));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format(UNABLE_TO_START_CONNECTOR, connector.getScheme(), connector.getPort(), ExceptionUtils.getMessage(e)), e);
            }
        }
    }

    /**
     * Add any required connectors to the DB.
     *
     * @param currentConnectors connectors currently in the database
     * @param actuallyStartThem if true, start each connector immediately after saving it.
     */
    private void createRequiredConnectors(Collection<SsgConnector> currentConnectors, boolean actuallyStartThem) {
        Collection<SsgConnector> toAdd = DefaultHttpConnectors.getRequiredConnectors(currentConnectors);
        for (SsgConnector connector : toAdd) {
            try {
                ssgConnectorManager.save(connector);
                if (actuallyStartThem) {
                    addConnector(connector);
                }
            } catch (SaveException e) {
                LOGGER.log(Level.WARNING, "Unable to save required connector to DB: " + ExceptionUtils.getMessage(e), e);
            } catch (ListenerException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                LOGGER.log(Level.WARNING, String.format(UNABLE_TO_START_CONNECTOR, connector.getScheme(), connector.getPort(), ExceptionUtils.getMessage(e)), ExceptionUtils.getDebugException(e));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format(UNABLE_TO_START_CONNECTOR, connector.getScheme(), connector.getPort(), ExceptionUtils.getMessage(e)), e);
            }
        }
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
            LOGGER.log(Level.WARNING, "HttpTransportModule is ignoring non-HTTP(S) connector with scheme " + scheme);
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
        Map<String, Object> m = new LinkedHashMap<>();

        m.put("maxThreads", "150");
        m.put("minSpareThreads", "25");
        m.put("maxSpareThreads", "75");
        m.put("disableUploadTimeout", "true");
        m.put("acceptCount", "100");

        String bindAddress = c.getProperty(PROP_BIND_ADDRESS);
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
                m.put(CLIENT_AUTH, "true");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_OPTIONAL)
                m.put(CLIENT_AUTH, "want");
            else if (clientAuth == SsgConnector.CLIENT_AUTH_NEVER)
                m.put(CLIENT_AUTH, "false");

        } else {
            // Not SSL
            m.put("connectionTimeout", "20000");
            m.put("socketFactory", "com.l7tech.server.tomcat.SsgServerSocketFactory");
        }

        // Allow overrides of any parameters we didn't already handle
        List<String> handledProperties = asList(PROP_BIND_ADDRESS, PROP_PORT_RANGE_START, PROP_PORT_RANGE_COUNT, PROP_TLS_CIPHERLIST, PROP_TLS_PROTOCOLS);
        List<String> extraPropNames = c.getPropertyNames();
        for (String name : extraPropNames) {
            if (handledProperties.contains(name)) {
                continue;
            }
            String value = c.getProperty(name);
            if (value != null) {
                m.put(name, value);
            }
        }

        return m;
    }

    @Override
    protected void removeConnector(Goid goid) {
        final Pair<SsgConnector, Connector> entry;
        entry = activeConnectors.remove(goid);
        if (entry == null) {
            return;
        }
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
        if (isStarted()) {
            return;
        }
        registerProtocols();
        startServletEngine();
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
                LOGGER.log(Level.SEVERE, "Unable to start HTTP connectors", e);
            }
        } else if (applicationEvent instanceof BundleBootstrapCompleteEvent){
            try {
                startInitialConnectors(false); // ensure we can find some initial connectors but don't start them yet (Bug #4500)
            } catch (ListenerException e) {
                LOGGER.log(Level.SEVERE, "Unable to start HTTP connectors", e);
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
            // DE286454 : adding the Private Thread Pool feature for HTTP listen ports
            ((Http11Protocol)ph).setExecutor( createAndRegisterExecutorIfRequired( executorName(ssgConn), ssgConn ) );
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
                LOGGER.log(Level.WARNING, "Unable to decrypt encrypted password in server.xml -- falling back to password from keystore.properties: " + ExceptionUtils.getMessage(e));
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
                LOGGER.warning("Ignoring invalid thread pool size '"+sizeStr+"' for connector '"+connector.getName()+"'.");
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
        activeConnectors.put(connector.getGoid(), new Pair<>(connector, c));

        int poolSize = connector.getIntProperty( PROP_THREAD_POOL_SIZE, 0 );
        AtomicInteger poolCounter = poolSize == 0 ? sharedThreadPoolConcurrency : new AtomicInteger();
        connectorPoolConcurrency.put( connector.getGoid(), poolCounter );

        int warningThreshold = connector.getIntProperty( PROP_CONCURRENCY_WARNING_THRESHOLD, 0 );
        if ( warningThreshold > 0 ) {
            connectorConcurrencyWarningThreshold.put( connector.getGoid(), warningThreshold );
        } else {
            connectorConcurrencyWarningThreshold.remove( connector.getGoid() );
        }

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

    @VisibleForTesting
    static void setConnectorAttributes( final Connector c, final Map<String, Object> attrs ) {
        // Ensure that we set the server header default if not specified for this connector
        if (attributeMissingOrEmpty(PROP_NAME_SERVER, attrs, EMPTY)) {
            attrs.put(PROP_NAME_SERVER, getDefaultServerHeader());
        }

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

    /**
     * Check if an atrribute in the attribute map is missing, null or considered empty against some value.
     *
     * @param attributeName attribute to be checked
     * @param attrs attributes map
     * @param emptyValue value to consider empty, "" for strings, 0 for integers.
     * @return boolean
     */
    private static boolean attributeMissingOrEmpty(final String attributeName, final Map<String, Object> attrs, final Object emptyValue) {
        Object attribute = attrs.get(attributeName);
        if (attribute == null) {
            return true;
        }
        if (attribute instanceof String) {
            attribute = ((String) attribute).trim();
        }
        return attribute.equals(emptyValue);
    }

    /**
     * Obtain the default server attribute value, from a system property or the default value for the gateway
     *
     * @return default server attribute value
     */
    private static String getDefaultServerHeader() {
        // try from the config, if not available build a default string description for the gateway
        String serverHeader = getProperty(HEADER_SERVER_CONFIG_PROP_DEFAULT);
        if (isEmpty(serverHeader)) {
            serverHeader = format(HEADER_SERVER_DEFAULT_FORMAT, getProductVersionMajor());
        }
        return serverHeader;
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
        if (testMode) {
            return testConnector;
        }

        Goid connectorGoid = (Goid)req.getAttribute(ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID);
        if (connectorGoid == null) {
            LOGGER.log(Level.WARNING, String.format(REQUEST_LACKS_ATTRIBUTE, ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID));
            return null;
        }

        Long htmId = (Long)req.getAttribute(ConnectionIdValve.ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID);
        HttpTransportModule htm = getInstance(htmId);
        if (htm == null) {
            LOGGER.log(Level.WARNING, String.format(REQUEST_LACKS_ATTRIBUTE, ConnectionIdValve.ATTRIBUTE_TRANSPORT_MODULE_INSTANCE_ID));
            return null;
        }

        Pair<SsgConnector, Connector> pair = htm.activeConnectors.get(connectorGoid);
        if (pair == null) {
            LOGGER.log(Level.WARNING, String.format(REQUEST_LACKS_ATTRIBUTE, ConnectionIdValve.ATTRIBUTE_CONNECTOR_OID +
                    ": No active connector with oid " + connectorGoid));
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
        if (testMode) {
            return testModule;
        }
        
        Reference<HttpTransportModule> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }

    /**
     * Dispatch notifications for endpoint activation.
     * @param connector the connector that was just activated. required.
     */
    private void notifyEndpointActivation( final SsgConnector connector ) {
        getApplicationContext().publishEvent( new SsgConnectorActivationEvent( this, connector ) );
    }

    /**
     * Dispatch notifications for endpoint deactivation.
     * @param connector the connector that was just deactivated. required.
     */
    private void notifyEndpointDeactivation( final SsgConnector connector ) {
        getApplicationContext().publishEvent( new SsgConnectorDeactivationEvent( this, connector ) );
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
        LOGGER.log(Level.WARNING, "Shutting down HTTP connector OID " + connectorGoid + desc + " because it cannot be opened with its current configuration");
        removeConnector(connectorGoid);
    }

    public int incrementConcurrencyForConnector( Goid connectorGoid ) {
        AtomicInteger counter = connectorPoolConcurrency.get( connectorGoid );
        if ( null == counter )
            counter = sharedThreadPoolConcurrency;

        int concurrency = counter.incrementAndGet();

        Integer warningThreshold = connectorConcurrencyWarningThreshold.get( connectorGoid );
        if ( warningThreshold != null && concurrency >= warningThreshold )
            maybeFireConcurrencyWarning( connectorGoid, concurrency);

        return concurrency;
    }

    private void maybeFireConcurrencyWarning(Goid connectorGoid, int concurrency) {
        int repeatDelay = ConfigFactory.getIntProperty( CONFIG_PROP_CONCURRENCY_WARNING_REPEAT_DELAY_SEC, 60 );
        long now = System.currentTimeMillis();
        long last = lastConcurrencyWarningTime.get();
        long millisSinceLast = now - last;
        if ( millisSinceLast < ( 1000L * repeatDelay ) ) {
            LOGGER.finer( "Suppressing repeated connector concurrency warning audit" );
            return;
        }

        if ( last == lastConcurrencyWarningTime.getAndSet( now ) ) {
            final TransportEvent event = new TransportEvent(
                    this,
                    component,
                    null,
                    Level.INFO,
                    "Concurrency Exceeded",
                    "Listener concurrency exceeded: " + concurrency );

            auditTransportEvent( event,
                    SystemMessages.CONNECTOR_CONCURRENCY_WARNING,
                    new String[] { connectorGoid.toString(), String.valueOf( concurrency ) },
                    null );
        }
    }

    public int decrementConcurrencyForConnector( Goid connectorGoid ) {
        AtomicInteger counter = connectorPoolConcurrency.get( connectorGoid );
        if ( null == counter )
            counter = sharedThreadPoolConcurrency;
        return counter.decrementAndGet();
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
