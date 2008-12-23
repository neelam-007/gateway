package com.l7tech.server.ems;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.resource.Resource;
import org.mortbay.log.Log;
import org.mortbay.log.Slf4jLog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.apache.wicket.protocol.http.WicketFilter;

import javax.servlet.Filter;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManager;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.server.util.FirewallUtils;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ems.enterprise.MappingFilter;

/**
 * An embedded servlet container that the EMS uses to host itself.
 *
 * TODO [steve] HTTP Cookies are not secure, needs to be configured here
 */
public class EsmServletContainer implements ApplicationContextAware, InitializingBean, DisposableBean, PropertyChangeListener {
    public static final String RESOURCE_PREFIX = "com/l7tech/server/ems/resources/";
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    private static final Logger logger = Logger.getLogger(EsmServletContainer.class.getName());

    private static final AtomicLong nextInstanceId = new AtomicLong(1);
    private static final Map<Long, Reference<EsmServletContainer>> instancesById =
            new ConcurrentHashMap<Long, Reference<EsmServletContainer>>();

    private final ServerConfig serverConfig;
    private final DefaultKey defaultKey;
    private final Timer timer;
    private final long instanceId;
    private final File temp;
    private ApplicationContext applicationContext;
    private Server server;
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

        File temp;
        File var = new File("var");
        if ( !var.exists() ) {
            temp = new File("/tmp");
        } else {
            temp = new File( "var/tmp" );
            temp.mkdir();
        }
        this.temp = temp;

        try {
            Log.setLog( new Slf4jLog( EsmServletContainer.class.getName() + ".SERVLET" ) );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error installing Jetty logger.", e );        
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
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

    private void persistConfiguration( final ListenerConfiguration configuration ) {
        String propfilePath = System.getProperty(ServerConfig.PROPS_OVER_PATH_PROPERTY, "var/emconfig.properties");
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
    }

    private void initializeServletEngine() throws Exception {
        timer.schedule( new TimerTask(){
            @Override
            public void run() {
                doRebuildConnectorsIfRequired();
            }
        }, 15000, 10000 );

        server = new Server();

        final ListenerConfiguration config = buildConfiguration();
        configuration.set( config );
        rebuildConnectors( config );

        final Context root = new Context(server, "/", Context.SESSIONS);
        root.setBaseResource(Resource.newClassPathResource("com/l7tech/server/ems/resources")); //TODO [steve] map root elsewhere and add other mappings for css/images/etc
        root.setDisplayName("Layer 7 Enterprise Service Manager Server");
        root.setAttribute("javax.servlet.context.tempdir", temp);
        root.addEventListener(new EsmContextLoaderListener());
        root.setClassLoader(Thread.currentThread().getContextClassLoader());

        //noinspection unchecked
        final Map<String, String> initParams = root.getInitParams();
        initParams.put("contextConfigLocation", "classpath:com/l7tech/server/ems/resources/webApplicationContext.xml");
        initParams.put(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));
        initParams.put("org.mortbay.jetty.servlet.Default.dirAllowed", "false");

        // Add security handler
        final Filter securityFilter = new EsmSecurityFilter();
        FilterHolder fsHolder = new FilterHolder(securityFilter);
        root.addFilter(fsHolder, "/*", Handler.REQUEST);

        // Add mapping handler
        final Filter mappingFilter = new MappingFilter();
        FilterHolder fmHolder = new FilterHolder(mappingFilter);
        root.addFilter(fmHolder, "/Configure.html", Handler.REQUEST);

        // Add wicket handler
        final WicketFilter wicketFilter = new WicketFilter();
        FilterHolder fHolder = new FilterHolder(wicketFilter);
        fHolder.setInitParameter("applicationClassName", EsmApplication.class.getName());
        fHolder.setName("wicketFilter");
        root.addFilter(fHolder, "/*", Handler.REQUEST);

        File webRoot = serverConfig.getLocalDirectoryProperty("em.server.webDirectory", false);
        if ( webRoot.exists() ) {
            final Resource _resourceBase = Resource.newResource( webRoot.toURI().toURL(), false );
            DefaultServlet resourceServlet = new DefaultServlet(){
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
            ServletHolder resourceHolder = new ServletHolder(resourceServlet);
            root.addServlet(resourceHolder, "/help/*");
        } else {
            logger.config("Ignoring invalid static content directory '"+webRoot.getAbsolutePath()+"'.");
        }


        //Set DefaultServlet to handle all static resource requests
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder defaultHolder = new ServletHolder(defaultServlet);
        root.addServlet(defaultHolder, "/");

        server.start();
        runningConfiguration.set( config );
    }

    private void shutdownServletEngine() throws Exception {
        server.stop();
        server.destroy();
        
        FirewallUtils.closeFirewallForConnectors( temp );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initializeServletEngine();
    }

    private ListenerConfiguration buildConfiguration() throws IOException {
        String addr = this.serverConfig.getProperty("em.server.listenaddr");
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
            logger.log( Level.WARNING, "Error installing listener configuration '"+desiredConfig+"'.", e );

            if ( actualConfg != null ) {
                logger.log( Level.INFO, "Reverting to previously used listener configuration '"+actualConfg+"'." );
                try {
                    rebuildConnectors( actualConfg );
                    configuration.set( actualConfg );
                } catch ( IOException ioe2 ) {
                    logger.log( Level.WARNING, "Error reverting listener configuration '"+desiredConfig+"'.", ioe2 );
                }
            }
        }
    }

    private void rebuildConnectors( final ListenerConfiguration configuration ) throws IOException {
        logger.info("Building HTTPS listener '"+configuration.getIpAddress()+":"+configuration.getHttpsPort()+"'.");

        boolean enableHttp = SyspropUtil.getBoolean("com.l7tech.ems.enableHttpListener");

        //
        // Create new connectors
        //
        Collection<SocketConnector> connectors = new ArrayList<SocketConnector>();
        try {
            final SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init( defaultKey.getSslKeyManagers(), null, null );

            SslSocketConnector sslConnector = new SslSocketConnector(){
                @Override
                protected SSLServerSocketFactory createFactory() throws Exception {
                    return ctx.getServerSocketFactory();
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
     * <p/>
     * This is normally used by the SsgJSSESocketFactory to locate a Connector's owner HttpTransportModule
     * so it can get at the SsgKeyStoreManager.
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
        private final String ipAddress;
        private final int httpPort;
        private final int httpsPort;

        ListenerConfiguration( final DefaultKey sslKey,
                               final String ipaddress,
                               final int httpPort,
                               final int httpsPort ) throws IOException {
            this.certificate = sslKey.getSslInfo().getCertificate();
            this.keyManagers = sslKey.getSslKeyManagers();
            this.ipAddress = ipaddress;
            this.httpPort = httpPort;
            this.httpsPort = httpsPort;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }

        public KeyManager[] getKeyManagers() {
            return keyManagers;
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
