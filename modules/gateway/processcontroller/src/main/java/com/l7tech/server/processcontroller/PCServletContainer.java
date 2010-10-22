package com.l7tech.server.processcontroller;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.Pair;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.processcontroller.ConfigService.DEFAULT_SSL_REMOTE_MANAGEMENT_PORT;

/**
 * An embedded servlet container that the PC uses to host itself.
 */
public class PCServletContainer implements ApplicationContextAware, InitializingBean, DisposableBean {
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    @SuppressWarnings({ "UnusedDeclaration" })
    private static final Logger logger = Logger.getLogger(PCServletContainer.class.getName());

    private static final AtomicLong nextInstanceId = new AtomicLong(1);
    private static final Map<Long, Reference<PCServletContainer>> instancesById =
            new ConcurrentHashMap<Long, Reference<PCServletContainer>>();

    private final long instanceId;
    private final int httpPort;
    private final String httpIPAddress;
    private ApplicationContext applicationContext;
    private Server server;
    private final ConfigService configService;

    public PCServletContainer(ConfigService configService) {
        this.instanceId = nextInstanceId.getAndIncrement();
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<PCServletContainer>(this));

        this.httpPort = configService.getSslPort();
        this.httpIPAddress = configService.getSslIPAddress();
        this.configService = configService;

        try {
            Log.setLog( new JulLogger(PCServletContainer.class.getName() + ".SERVLET") );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error installing Jetty logger.", e );
        }
    }

    private void initializeServletEngine() throws Exception {
        Server server = new Server();
        Pair<X509Certificate[],PrivateKey> keypair = configService.getSslKeypair();
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[] { new SingleCertX509KeyManager(keypair.left, keypair.right) }, new TrustManager[]{ new X509TrustManager(){
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                throw new CertificateException();
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } }, null);
        final SslSocketConnector sslConnector = new SslSocketConnector() {
            @Override
            protected SSLServerSocketFactory createFactory() throws Exception {
                return ctx.getServerSocketFactory();
            }

            @Override
            public int getPort() {
                return httpPort;
            }

            @Override
            public String getHost() {
                return httpIPAddress;
            }
        };
        sslConnector.setWantClientAuth(true);
        sslConnector.setNeedClientAuth(false);
        server.addConnector(sslConnector);

        InetAddress httpAddr = InetAddress.getByName(httpIPAddress);

        if (InetAddressUtil.isIpv4Enabled()) {
            if ( httpPort != DEFAULT_SSL_REMOTE_MANAGEMENT_PORT || !(httpAddr instanceof Inet4Address) ||
                ! ( httpAddr.isLoopbackAddress() || httpAddr.isAnyLocalAddress() )  ) {

                final SslSocketConnector localSslConnector = new SslSocketConnector() {
                    @Override
                    protected SSLServerSocketFactory createFactory() throws Exception {
                        return ctx.getServerSocketFactory();
                    }

                    @Override
                    public int getPort() {
                        return DEFAULT_SSL_REMOTE_MANAGEMENT_PORT;
                    }

                    @Override
                    public String getHost() {
                        return "127.0.0.1";
                    }
                };
                server.addConnector( localSslConnector );
                logger.log(Level.INFO, "Added default IPv4 SSL connector.");
            }
        }

        if ( InetAddressUtil.isIpv6Enabled() ) {
            if ( httpPort != DEFAULT_SSL_REMOTE_MANAGEMENT_PORT || ! (httpAddr instanceof Inet6Address) ||
                 ! ( httpAddr.isLoopbackAddress() || httpAddr.isAnyLocalAddress() )  ) {

                logger.log(Level.INFO, "Adding default IPv6 connector.");
                final SslSocketConnector localSslConnector = new SslSocketConnector() {
                    @Override
                    protected SSLServerSocketFactory createFactory() throws Exception {
                        return ctx.getServerSocketFactory();
                    }

                    @Override
                    public int getPort() {
                        return DEFAULT_SSL_REMOTE_MANAGEMENT_PORT;
                    }

                    @Override
                    public String getHost() {
                        return "::1";
                    }
                };
                server.addConnector( localSslConnector );
                logger.log(Level.INFO, "Added default IPv6 SSL connector.");
            }
        }

        final Context root = new Context(server, "/", Context.SESSIONS);
        root.setBaseResource(Resource.newClassPathResource("com/l7tech/server/processcontroller/resources/web"));
        root.setDisplayName("Layer 7 Process Controller");
        final File var = new File("var");
        final File varTmp = new File(var, "tmp");
        final File varRun = new File(var, "run");
        if ( var.exists() && (varTmp.exists() || varTmp.mkdir()) ) {
            root.setAttribute("javax.servlet.context.tempdir", varTmp);            
        } else {
            root.setAttribute("javax.servlet.context.tempdir", new File("/tmp"));
        }
        root.addEventListener(new PCContextLoaderListener());
        root.setClassLoader(Thread.currentThread().getContextClassLoader());

        //Write certificate to file
        if ( var.exists() && (varRun.exists() || varRun.mkdir()) ) {
            final File certificate = new File( varRun, "pc.cer");
            try {
                FileUtils.save( new ByteArrayInputStream(keypair.left[0].getEncoded()), certificate );
                certificate.setReadable( true, false );
            } catch ( CertificateEncodingException e ) {
                logger.warning( "Unable to save certificate file '"+ExceptionUtils.getMessage( e )+"'." );
            } catch ( IOException e ) {
                logger.warning( "Unable to save certificate file '"+ExceptionUtils.getMessage( e )+"'." );
            }
        }

        //noinspection unchecked
        final Map<String, String> initParams = root.getInitParams();
        initParams.put("contextConfigLocation", "classpath:com/l7tech/server/processcontroller/resources/processControllerWebApplicationContext.xml");
        initParams.put(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));

        final CXFServlet cxfServlet = new CXFServlet();
        final ServletHolder cxfHolder = new ServletHolder(cxfServlet);
        root.addServlet(cxfHolder, configService.getServicesContextBasePath() + "/*");

        //Set DefaultServlet to handle all static resource requests
        final DefaultServlet defaultServlet = new DefaultServlet();
        final ServletHolder defaultHolder = new ServletHolder(defaultServlet);
        root.addServlet(defaultHolder, "/");

        server.start();
        this.server = server;
    }

    private void shutdownServletEngine() throws Exception {
        server.stop();
        server.destroy();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initializeServletEngine();
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
    public static PCServletContainer getInstance(long id) {
        Reference<PCServletContainer> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }

    private static final class JulLogger implements org.mortbay.log.Logger {
        private final Logger logger;

        public JulLogger( final String name ) {
            logger = Logger.getLogger( name );
        }

        @Override
        public void debug(String msg, Object arg0, Object arg1) {
            logger.log( Level.FINE, msg, new Object[]{arg0, arg1} );
        }

        @Override
        public void debug(String msg, Throwable th) {
            logger.log( Level.FINE, msg, ExceptionUtils.getDebugException(th) );
        }

        @Override
        public org.mortbay.log.Logger getLogger(String name) {
            return new JulLogger(name);
        }

        @Override
        public void info(String msg, Object arg0, Object arg1) {
            logger.log( Level.INFO, msg, new Object[]{arg0, arg1} );
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void setDebugEnabled(boolean enabled) {
        }

        @Override
        public void warn(String msg, Object arg0, Object arg1) {
            logger.log( Level.WARNING, msg, new Object[]{arg0, arg1} );
        }

        @Override
        public void warn(String msg, Throwable th) {
            logger.log( Level.WARNING, msg, th );
        }
    }
}
