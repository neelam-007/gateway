package com.l7tech.server.ems.listener;

import static com.l7tech.common.io.SSLServerSocketFactoryWrapper.wrapAndSetTlsVersionAndCipherSuites;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.ems.enterprise.MappingFilter;
import com.l7tech.server.ems.ui.EsmApplication;
import com.l7tech.server.ems.ui.EsmSecurityFilter;
import com.l7tech.server.ems.ui.EsmSessionServlet;
import com.l7tech.server.util.FirewallUtils;
import com.l7tech.util.*;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.split;
import static com.l7tech.util.TextUtils.trim;
import org.apache.wicket.protocol.http.WicketFilter;
import org.mortbay.jetty.*;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ErrorHandler;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.*;
import org.mortbay.log.Log;
import org.mortbay.log.Slf4jLog;
import org.mortbay.resource.Resource;
import org.mortbay.util.StringUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.BindException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * An embedded servlet container that the ESM uses to host itself.
 */
public class EsmServletContainer implements ApplicationContextAware, InitializingBean, DisposableBean, PropertyChangeListener {
    public static final String RESOURCE_PREFIX = "com/l7tech/server/ems/resources/";
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    private static final Logger logger = Logger.getLogger(EsmServletContainer.class.getName());

    private static final AtomicLong nextInstanceId = new AtomicLong(1);
    private static final Map<Long, Reference<EsmServletContainer>> instancesById =
            new ConcurrentHashMap<Long, Reference<EsmServletContainer>>();
    private static final Pattern SPLITTER = Pattern.compile("\\s*,\\s*");

    private static final long DEFAULT_SESSION_TIMEOUT = 1800000L; // session idle timeout in ms
    private static final String DEFAULT_LISTENPORT_CIPHERS = "TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_RC4_128_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA";
    private static final String PROP_SESSION_TIMEOUT = "em.server.session.timeout";
    private static final String PROP_LISTENPORT_PROTOCOLS = "em.server.listenport.protocols";
    private static final String PROP_LISTENPORT_CIPHERS = "em.server.listenport.ciphers";

    private final ServerConfig serverConfig;
    private final DefaultKey defaultKey;
    private final Timer timer;
    private final long instanceId;
    private final File temp;
    private ApplicationContext applicationContext;
    private Server server;
    private SessionManager sessionManager;
    private Audit audit;
    private AtomicReference<ListenerConfiguration> runningConfiguration = new AtomicReference<ListenerConfiguration>();  // config in use
    private AtomicReference<ListenerConfiguration> configuration = new AtomicReference<ListenerConfiguration>(); // config desired

    public EsmServletContainer( final ServerConfig serverConfig,
                                final DefaultKey defaultKey,
                                final Timer timer ) {
        this.serverConfig = serverConfig;
        this.defaultKey = defaultKey;
        this.timer = timer;
        this.instanceId = nextInstanceId.getAndIncrement();
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<EsmServletContainer>(this));
        this.temp = getTempFilesDirectory();

        try {
            Log.setLog( new Slf4jLog( EsmServletContainer.class.getName() + ".SERVLET" ) );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error installing Jetty logger.", e );        
        }
        Resource.setDefaultUseCaches(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ( evt != null && PROP_SESSION_TIMEOUT.equals(evt.getPropertyName()) ) {
            if ( sessionManager != null ) {
                int sessionTimeoutSeconds = (int)(serverConfig.getTimeUnitProperty( PROP_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT )/1000L);
                logger.config( "Updated session timeout configuration, now " + sessionTimeoutSeconds + " seconds.");
                sessionManager.setMaxInactiveInterval( sessionTimeoutSeconds );            
            }
        } else {
            if ( runningConfiguration.get() != null && server.isStarted() ) {
                // schedule change for later so any current HTTP request can complete before the
                // listener goes offline
                timer.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        // build a new config and put it on the config stack, will be
                        // picked up later.
                        try {
                            ListenerConfiguration config = buildConfiguration();
                            ListenerConfiguration currentConfig = configuration.get();

                            if ( currentConfig != null && !config.equals( currentConfig ) ) {
                                // the config is updated so use it
                                if ( configuration.compareAndSet( currentConfig, config ) ) {
                                    logger.info( "Queued updated listener configuration : " + config );
                                } else {
                                    logger.warning("Failed to queue updated listener configuration : " + config);
                                }
                            } else {
                                logger.fine( "Ignoring edited listener configuration that matches current configuration : " + config  + " - " + currentConfig);
                            }

                        } catch (IOException e) {
                            logger.log( Level.WARNING, "Error building new listener configuration.", e );
                        }
                    }
                }, 500);
            }
        }
    }

    private void persistConfiguration( final ListenerConfiguration configuration ) {
        String propfilePath = "var/emconfig.properties";
        File propertyFile = new File( propfilePath );
        if ( propertyFile.exists() && propertyFile.canWrite() ) {
            InputStream in = null;
            OutputStream out = null;
            try {
                // load existing properties
                Properties properties = new Properties();
                in = new FileInputStream( propertyFile );
                properties.load( in );
                ResourceUtils.closeQuietly( in ); in = null;

                // update
                properties.setProperty( "em.server.listenport", Integer.toString(configuration.getHttpsPort()) );
                properties.setProperty( "em.server.listenaddr", configuration.getIpAddress() );

                // save
                out = new FileOutputStream( propertyFile );
                properties.store( out, "" );
            } catch ( IOException ioe ) {
                logger.log( Level.WARNING, "Error updating emconfig properties.", ioe );
            } finally {
                ResourceUtils.closeQuietly( in );
                ResourceUtils.closeQuietly( out );    
            }
        }

        applicationContext.publishEvent( new ListenerConfigurationUpdatedEvent(
                this,
                configuration.getIpAddress(),
                configuration.getHttpsPort(),
                configuration.getAlias()) );
    }

    private void initializeServletEngine() throws Exception {
        timer.schedule( new TimerTask(){
            @Override
            public void run() {
                doRebuildConnectorsIfRequired();
            }
        }, 5000, 15000 );

        server = new Server();

        final ListenerConfiguration config = buildConfiguration();
        configuration.set( config );

        final Context root = new Context(server, "/", Context.SESSIONS);
        AbstractSessionManager sessionManager = ((AbstractSessionManager)root.getSessionHandler().getSessionManager());
        sessionManager.setSecureCookies(true);
        sessionManager.setMaxInactiveInterval((int)(serverConfig.getTimeUnitProperty( PROP_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT )/1000L));
        this.sessionManager = sessionManager;
        root.setBaseResource(Resource.newClassPathResource("com/l7tech/server/ems/resources/web"));
        root.setDisplayName("Layer 7 Enterprise Service Manager Server");
        root.setAttribute("javax.servlet.context.tempdir", temp);
        root.addEventListener(new EsmContextLoaderListener());
        root.setClassLoader(Thread.currentThread().getContextClassLoader());

        //noinspection unchecked
        final Map<String, String> initParams = root.getInitParams();
        initParams.put("contextConfigLocation", "classpath:com/l7tech/server/ems/resources/webApplicationContext.xml");
        initParams.put(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));
        initParams.put("org.mortbay.jetty.servlet.Default.dirAllowed", "false");
        initParams.put("org.mortbay.jetty.servlet.Default.cacheControl", "max-age=1800");        
        initParams.put("org.mortbay.jetty.servlet.SessionCookie", "ESMSESSIONID");
        initParams.put("org.mortbay.jetty.servlet.SessionURL", "none"); // disable url sessionid
//        <Set class="org.eclipse.jetty.util.resource.Resource" name="defaultUseCaches">false</Set>


        // Add security handler
        final Filter securityFilter = new EsmSecurityFilter();
        FilterHolder fsHolder = new FilterHolder(securityFilter);
        root.addFilter(fsHolder, "/*", Handler.REQUEST);

        // Add mapping handler
        final Filter mappingFilter = new MappingFilter();
        FilterHolder fmHolder = new FilterHolder(mappingFilter);
        root.addFilter(fmHolder, "/Configure.html", Handler.REQUEST);
        root.addFilter(fmHolder, "/PolicyMigration.html", Handler.REQUEST);
        root.addFilter(fmHolder, "/StandardReports.html", Handler.REQUEST);

        // Add wicket handler
        final WicketFilter wicketFilter = new WicketFilter();
        FilterHolder fHolder = new FilterHolder(wicketFilter);
        fHolder.setInitParameter("applicationClassName", EsmApplication.class.getName());
        fHolder.setName("wicketFilter");
        root.addFilter(fHolder, "/*", Handler.REQUEST);

        File webRoot = serverConfig.getLocalDirectoryProperty("em.server.webDirectory", false);
        if ( webRoot.exists() ) {
            DefaultServlet resourceServlet = buildResourceServlet(webRoot);
            ServletHolder resourceHolder = new ServletHolder(resourceServlet);
            root.addServlet(resourceHolder, "/help/*");
        } else {
            logger.config("Ignoring invalid static content directory '"+webRoot.getAbsolutePath()+"'.");
        }

        // Set DefaultServlet to handle all static resource requests
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder defaultHolder = new ServletHolder(defaultServlet);
        root.addServlet(defaultHolder, "/");

        // Add session servlet (no mapping, will be invoked by name)
        EsmSessionServlet esmSessionServlet = new EsmSessionServlet();
        ServletHolder esmSessionServletHolder = new ServletHolder(esmSessionServlet);
        esmSessionServletHolder.setName("sessionServlet");
        root.getServletHandler().addServlet(esmSessionServletHolder);

        root.setErrorHandler( buildErrorHandler() );

        server.start();
    }

    private void shutdownServletEngine() throws Exception {
        server.stop();
        server.destroy();
        
        FirewallUtils.closeFirewallForConnectors( temp );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cleanTempFiles();
        initializeServletEngine();
    }

    private void cleanTempFiles() {
        File wicketFilestore = new File(temp, "wicketFilter-filestore");
        if ( wicketFilestore.exists() ) {
            logger.info("Deleting old temporary files.");
            FileUtils.deleteDir( wicketFilestore );
        }
    }

    private ListenerConfiguration buildConfiguration() throws IOException {
        String addr = this.serverConfig.getProperty( "em.server.listenaddr" );
        int httpPort = this.serverConfig.getIntProperty("em.server.listenportdev", 8181);
        int httpsPort = this.serverConfig.getIntProperty("em.server.listenport", 8182);

        return new ListenerConfiguration( defaultKey, addr, httpPort, httpsPort );
    }

    private void doRebuildConnectorsIfRequired() {
        final ListenerConfiguration desiredConfig = configuration.get();
        final ListenerConfiguration actualConfg = runningConfiguration.get();
        try {

            if ( desiredConfig != null && (actualConfg == null || !desiredConfig.equals(actualConfg)) ) {
                logger.info("Configuration is updated applying new configuration.");
                rebuildConnectors( desiredConfig );
                runningConfiguration.set( desiredConfig );
                persistConfiguration( desiredConfig ); // store updated config on success
            }

        } catch (IOException e) {
            if ( ExceptionUtils.causedBy( e, BindException.class ) ) {
                BindException be = ExceptionUtils.getCauseIfCausedBy( e, BindException.class );
                logger.log( Level.WARNING, "Error installing listener configuration '"+desiredConfig+"', due to '"+ExceptionUtils.getMessage(be)+"'." );
            } else {
                logger.log( Level.WARNING, "Error installing listener configuration '"+desiredConfig+"'.", e );
            }

            if ( actualConfg != null ) {
                logger.log( Level.INFO, "Reverting to previously used listener configuration '"+actualConfg+"'." );
                try {
                    rebuildConnectors( actualConfg );
                    configuration.set( actualConfg );
                } catch ( IOException ioe2 ) {
                    if ( ExceptionUtils.causedBy( ioe2, BindException.class ) ) {
                        BindException be = ExceptionUtils.getCauseIfCausedBy( ioe2, BindException.class );
                        logger.log( Level.WARNING, "Error reverting listener configuration '"+desiredConfig+"', due to '"+ExceptionUtils.getMessage(be)+"'." );
                    } else {
                        logger.log( Level.WARNING, "Error reverting listener configuration '"+desiredConfig+"'.", ioe2 );
                    }
                }
            }
        }
    }

    private void rebuildConnectors( final ListenerConfiguration configuration ) throws IOException {
        logger.info("Building HTTPS listener '" + InetAddressUtil.getHostForUrl(configuration.getIpAddress()) + ":" + configuration.getHttpsPort() + "'.");

        boolean enableHttp = ConfigFactory.getBooleanProperty( "com.l7tech.ems.enableHttpListener", false );

        //
        // Create new connectors
        //
        Collection<SocketConnector> connectors = new ArrayList<SocketConnector>();
        try {
            final SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init( defaultKey.getSslKeyManagers(), null, null );

            SslSocketConnector sslConnector = new SslSocketConnector(){
                @Override
                protected SSLServerSocketFactory createFactory() throws Exception {
                    final Option<String[]> desiredProtocols = getStringArrayProperty( PROP_LISTENPORT_PROTOCOLS, null );
                    final Option<String[]> desiredCiphers = getStringArrayProperty( PROP_LISTENPORT_CIPHERS, DEFAULT_LISTENPORT_CIPHERS );
                    final SSLServerSocketFactory sslServerSocketFactory = ctx.getServerSocketFactory();
                    final Option<String[]> enabledCiphers = desiredCiphers.map( new Unary<String[],String[]>(){
                        @Override
                        public String[] call( final String[] ciphers ) {
                            return ArrayUtils.intersection(ciphers, sslServerSocketFactory.getSupportedCipherSuites());
                        }
                    } );
                    return wrapAndSetTlsVersionAndCipherSuites(
                            sslServerSocketFactory,
                            desiredProtocols.toNull(),
                            enabledCiphers.toNull() );
                }
                private Option<String[]> getStringArrayProperty( final String property, final String defaultValue ) {
                    return optional( serverConfig.getProperty( property ) )
                            .map( trim() )
                            .filter( isNotEmpty() )
                            .orElse( optional( defaultValue ) )
                            .map( split( SPLITTER ) );
                }
            };
            sslConnector.setPort( configuration.getHttpsPort() );
            sslConnector.setHost( configuration.getIpAddress() );
            connectors.add( sslConnector );

            if (enableHttp) {
                SocketConnector connector = new SocketConnector();
                connector.setPort( configuration.getHttpPort() );
                connector.setHost( configuration.getIpAddress() );
                connectors.add( connector );
            }
        } catch ( GeneralSecurityException gse ) {
            throw new CausedIOException( "Error when rebuilding HTTP(S) connector(s).", gse );
        }


        //
        // Audit stop old
        //
        Connector[] currentConnectors = server.getConnectors();
        if ( currentConnectors != null ) {
            for ( Connector connector : currentConnectors ) {
                if ( connector != null ) {
                    if ( audit != null ) {
                        if ( connector instanceof SslSocketConnector ) {
                            audit.logAndAudit( SystemMessages.HTTPSERVER_STOP, "HTTPS Port: " + connector.getPort());
                        } else {
                            audit.logAndAudit( SystemMessages.HTTPSERVER_STOP, "HTTP Port: " + connector.getPort());
                        }
                    }
                }
            }
        }

        //
        // Audit start new
        //
        List<SsgConnector> fireWallConnectors = new ArrayList<SsgConnector>();
        for ( SocketConnector connector : connectors ) {
            if ( audit != null ) {
                if ( connector instanceof SslSocketConnector ) {
                    audit.logAndAudit( SystemMessages.HTTPSERVER_START, "HTTPS Port: " + connector.getPort());
                } else {
                    audit.logAndAudit( SystemMessages.HTTPSERVER_START, "HTTP Port: " + connector.getPort());                    
                }
            }

            SsgConnector ssgConnector = new SsgConnector();
            ssgConnector.setPort(connector.getPort());
            fireWallConnectors.add(ssgConnector);
        }
        
        server.setConnectors( connectors.toArray( new Connector[connectors.size()] ) );        

        //
        // Stop old
        //
        if ( currentConnectors != null ) {
            for ( Connector connector : currentConnectors ) {
                if ( connector.isStarted() ) {
                    try {
                        connector.stop();
                    } catch ( Exception e ) {
                        logger.log( Level.WARNING, "Error stopping HTTP(S) connector.", e);                    
                    }
                }
            }
        }

        //
        // Start new if server is running (else will be started later).
        //
        if ( server.isStarted() ) {
            currentConnectors = server.getConnectors();
            if ( currentConnectors != null ) {
                try {
                    for ( Connector connector : currentConnectors ) {
                            connector.start();
                    }
                } catch ( Exception e ) {
                    throw new CausedIOException("Error starting HTTP(S) connector.", e);
                }
            }
        }

        FirewallUtils.openFirewallForConnectors( temp, fireWallConnectors );  // closes for old ports also
    }

    @Override
    public void destroy() throws Exception {
        shutdownServletEngine();
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.audit = new Auditor(this, applicationContext, logger);
    }

    private File getTempFilesDirectory() {
        File temp;

        File var = new File("var");
        if ( !var.exists() ) {
            temp = new File("/tmp");
        } else {
            temp = new File( "var/tmp" );
            temp.mkdir();
        }

        return temp;
    }

    private DefaultServlet buildResourceServlet( final File webRoot ) throws MalformedURLException {
        final Resource _resourceBase = Resource.newResource( webRoot.toURI().toURL(), false );
        return new DefaultServlet(){
            @Override
            public Resource getResource( final String pathInContext )
            {
                if (_resourceBase==null) return null;
                Resource r=null;
                try {
                    r = _resourceBase.addPath(pathInContext);

                } catch (IOException e) {
                    logger.log( Level.INFO, "Missing resource.", e );
                }
                return r;
            }
        };
    }

    private ErrorHandler buildErrorHandler() {
        ErrorHandler errorHandler = new ErrorHandler(){
            @Override
            protected void handleErrorPage( final HttpServletRequest request,
                                            final Writer writer,
                                            final int code,
                                            final String errorMessage) throws IOException {
                if ( code != 404 ) {
                    EsmServletContainer.this.writeErrorPage( request, writer, code, errorMessage, isShowStacks() );
                } else {
                    byte[] pageBytes = IOUtils.slurpStream( EsmApplication.class.getResourceAsStream("pages/EsmNotFound.html") );
                    writer.write( new String(pageBytes, Charsets.UTF8) );
                }
            }
        };
        errorHandler.setShowStacks( ConfigFactory.getBooleanProperty( "com.l7tech.ems.development", false ) );

        return errorHandler;
    }

    /**
     * Write basic error page. 
     */
    private void writeErrorPage( final HttpServletRequest request,
                                 final Writer writer,
                                 final int code,
                                 final String errorMessage,
                                 final boolean showStacks ) throws IOException  {
        String message;
        if ( errorMessage == null ) {
            message = HttpGenerator.getReason(code);
        } else {
            message = StringUtil.replace(errorMessage, "&", "&amp;");
            message = StringUtil.replace(message, "<", "&lt;");
            message = StringUtil.replace(message, ">", "&gt;");
        }

        writer.write("<html>\n<head>\n");
        writer.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\n");
        writer.write("<title>Error ");
        writer.write(Integer.toString(code));
        writer.write(' ');
        if ( message != null ) {
            writer.write(message);
        }
        writer.write("</title>\n");
        writer.write("</head>\n<body>");
        writer.write("<h2>HTTP ERROR: ");
        writer.write(Integer.toString(code));
        writer.write("</h2><pre>");
        writer.write(message);
        writer.write("</pre>");
        if ( showStacks ) {
            Throwable th = (Throwable)request.getAttribute("javax.servlet.error.exception");
            while(th!=null)
            {
                writer.write("<h3>Caused by:</h3><pre>");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                th.printStackTrace(pw);
                pw.flush();
                writer.write(sw.getBuffer().toString());
                writer.write("</pre>\n");

                th =th.getCause();
            }
        }
        writer.write("\n</body>\n</html>\n");
        for (int i= 0; i < 20; i++)
            writer.write("<br/>                                                \n");
    }

    /**
     * Get the number that can be used to uniquely identify this EmsServletContainer instance within its
     * classloader.  This number can later be passed to {@link #getInstance(long)} to retrieve a referene
     * to the instance.
     *
     * @return the instance ID of this EmsServletContainer instance.
     */
    long getInstanceId() {
        return instanceId;
    }

    /**
     * Find the EmsServletContainer corresponding to the specified instance ID.
     *
     * @see #getInstanceId
     * @param id the instance ID to search for.  Required.
     * @return  the corresopnding HttpTransportModule instance, or null if not found.
     */
    public static EsmServletContainer getInstance(long id) {
        Reference<EsmServletContainer> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }

    private static final class ListenerConfiguration {
        private final X509Certificate certificate;
        private final KeyManager[] keyManagers;
        private final String alias;
        private final String ipAddress;
        private final int httpPort;
        private final int httpsPort;

        ListenerConfiguration( final DefaultKey sslKey,
                               final String ipaddress,
                               final int httpPort,
                               final int httpsPort ) throws IOException {
            this.certificate = sslKey.getSslInfo().getCertificate();
            this.keyManagers = sslKey.getSslKeyManagers();
            this.alias = sslKey.getSslInfo().getAlias();
            this.ipAddress = "*".equals(ipaddress) ? InetAddressUtil.getAnyHostAddress() : ipaddress;
            this.httpPort = httpPort;
            this.httpsPort = httpsPort;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }

        public KeyManager[] getKeyManagers() {
            return keyManagers;
        }

        public String getAlias() {
            return alias;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getHttpPort() {
            return httpPort;
        }

        public int getHttpsPort() {
            return httpsPort;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ListenerConfiguration that = (ListenerConfiguration) o;

            if (httpPort != that.httpPort) return false;
            if (httpsPort != that.httpsPort) return false;
            if (!certificate.equals(that.certificate)) return false;
            if (!ipAddress.equals(that.ipAddress)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = certificate.hashCode();
            result = 31 * result + ipAddress.hashCode();
            result = 31 * result + httpPort;
            result = 31 * result + httpsPort;
            return result;
        }

        @Override
        public String toString() {
            return "Listener Configuration[http="+httpPort+"; https="+httpsPort+"; addr="+ ipAddress +"; sslCert="+certificate.getSubjectDN().getName()+";]";
        }
    }
}
