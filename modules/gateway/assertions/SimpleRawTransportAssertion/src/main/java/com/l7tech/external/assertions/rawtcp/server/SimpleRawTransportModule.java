package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.cert.TrustedCertServices;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.Component.GW_GENERIC_CONNECTOR;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_L7RAWTCP_MESSAGE_INPUT;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;

/**
 *
 */
public class SimpleRawTransportModule extends TransportModule implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(SimpleRawTransportModule.class.getName());
    protected static final String SCHEME_RAW_TCP = "l7.raw.tcp";

    private static final Set<String> SUPPORTED_SCHEMES = caseInsensitiveSet(SCHEME_RAW_TCP);

    private static BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<Runnable>();
    private static final int CORE_POOL_SIZE = 25;
    private static final int MAX_POOL_SIZE = 50;
    private static final long KEEPALIVE_SECONDS = 5L * 60L;
    private static ExecutorService requestExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEPALIVE_SECONDS, TimeUnit.SECONDS, requestQueue);

    private class ServerSock implements Closeable {
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final SsgConnector connector;
        private final ExecutorService executor;
        private final boolean executorRequiresClose;
        private final ServerSocket tcpSocket;
        private Thread listenThread;

        private ServerSock(SsgConnector connector, ServerSocket tcpSocket, ExecutorService executor, boolean executorRequiresClose) {
            this.connector = connector;
            this.tcpSocket = tcpSocket;
            this.executor = executor;
            this.executorRequiresClose = executorRequiresClose;
        }

        @Override
        public synchronized void close() throws IOException {
            try {
                closed.set(true);
                closeThread();
                closeExecutor();
                if (tcpSocket != null) tcpSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Exception closing connector: " + ExceptionUtils.getMessage(e), e);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error closing connector: " + ExceptionUtils.getMessage(e), e);
            }
        }

        private void closeThread() {
            if (listenThread != null) {
                listenThread.interrupt();
                listenThread = null;
            }
        }

        private void closeExecutor() {
            if (executor != null && executorRequiresClose) {
                executor.shutdown();
            }
        }

        public synchronized void start() {
            closeThread();
            String name = "TCP " + tcpSocket.getLocalPort();
            Runnable listenerRunnable = new Runnable() {
                @Override
                public void run() {
                    runTcpListener();
                }
            };
            listenThread = new Thread(listenerRunnable, "Raw transport listener thread " + name);
            listenThread.start();
        }

        private void runTcpListener() {
            try {
                // The acceptor thread loops as fast as it can, accepting connections and enqueuing them to be
                // handled by the executor.  The acceptor thread avoids issuing any blocking operations
                // (except when accept() blocks when there are no more pending incoming connections, or
                //  should the executor's submit() method blocks.)
                //noinspection InfiniteLoopStatement
                for (;;) {
                    if (closed.get())
                        break;

                    try {
                        final Socket sock = tcpSocket.accept();
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                handleRawTcpRequest(sock, connector);
                            }
                        });

                    } catch (IOException e) {
                        if (closed.get())
                            break;
                        logger.log(Level.WARNING, "Unable to accept raw TCP connection on port " + tcpSocket.getLocalPort() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        Thread.sleep(10000L);
                    }
                }
            } catch (InterruptedException e1) {
                logger.log(Level.FINE, "raw TCP listen thread interrupted; exiting");
            }
        }
    }

    private final ApplicationEventProxy applicationEventProxy;
    private final GatewayState gatewayState;
    private final MessageProcessor messageProcessor;
    private final StashManagerFactory stashManagerFactory;
    private final Map<Goid, Pair<SsgConnector, ServerSock>> activeConnectors = new ConcurrentHashMap<Goid, Pair<SsgConnector, ServerSock>>();

    public SimpleRawTransportModule(ApplicationEventProxy applicationEventProxy,
                                    LicenseManager licenseManager,
                                    SsgConnectorManager ssgConnectorManager,
                                    TrustedCertServices trustedCertServices,
                                    DefaultKey defaultKey,
                                    Config config,
                                    GatewayState gatewayState,
                                    MessageProcessor messageProcessor,
                                    StashManagerFactory stashManagerFactory)
    {
        super("Simple raw transport module", GW_GENERIC_CONNECTOR, logger, SERVICE_L7RAWTCP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, config );
        this.applicationEventProxy = applicationEventProxy;
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.stashManagerFactory = stashManagerFactory;
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        T got = beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass()))
            return got;
        throw new IllegalStateException("Unable to get bean from application context: " + beanName);

    }

    static SimpleRawTransportModule createModule( final ApplicationContext appContext ) {
        LicenseManager licenseManager = getBean(appContext, "licenseManager", LicenseManager.class);
        SsgConnectorManager ssgConnectorManager = getBean(appContext, "ssgConnectorManager", SsgConnectorManager.class);
        TrustedCertServices trustedCertServices = getBean(appContext, "trustedCertServices", TrustedCertServices.class);
        DefaultKey defaultKey = getBean(appContext, "defaultKey", DefaultKey.class);
        Config config = getBean(appContext, "serverConfig", Config.class);
        GatewayState gatewayState = getBean(appContext, "gatewayState", GatewayState.class);
        MessageProcessor messageProcessor = getBean(appContext, "messageProcessor", MessageProcessor.class);
        StashManagerFactory stashManagerFactory = getBean(appContext, "stashManagerFactory", StashManagerFactory.class);
        ApplicationEventProxy applicationEventProxy = getBean(appContext, "applicationEventProxy", ApplicationEventProxy.class);
        final SimpleRawTransportModule module = new SimpleRawTransportModule(applicationEventProxy, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, config, gatewayState, messageProcessor, stashManagerFactory);
        module.setApplicationContext( appContext );
        return module;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                auditError( SCHEME_RAW_TCP, "Unable to access initial raw connectors: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
            }
        }
    }

    @Override
    public void reportMisconfiguredConnector(Goid connectorOid) {
        // Ignore, can't currently happen for simple raw
        logger.log(Level.WARNING, "Raw connector reported misconfigured: OID " + connectorOid);
    }

    private void startInitialConnectors() throws FindException {
        final boolean wasSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            for (SsgConnector connector : connectors) {
                if (connector.isEnabled() && connectorIsOwnedByThisModule(connector)) {
                    try {
                        addConnector(connector);
                    } catch ( Exception e ) {
                        final Exception auditException;
                        if ( ExceptionUtils.getMessage(e).contains("java.net.BindException: ") ) { // The exception cause is not chained ...
                            auditException = ExceptionUtils.getDebugException(e);
                        } else {
                            auditException = e;
                        }
                        auditError( SCHEME_RAW_TCP, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                    ": " + ExceptionUtils.getMessage(e), auditException );
                    }
                }
            }

        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    @Override
    protected void doStart() throws LifecycleException {
        registerCustomProtocols();
        if (gatewayState.isReadyForMessages()) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                auditError( SCHEME_RAW_TCP, "Unable to access initial raw connectors: " + ExceptionUtils.getMessage(e), e );
            }
        }
    }

    void registerApplicationEventListener() {
        applicationEventProxy.addApplicationListener(this);
    }

    private void registerCustomProtocols() {
        TransportDescriptor tcpInfo = new TransportDescriptor();
        tcpInfo.setScheme(SCHEME_RAW_TCP);
        tcpInfo.setSupportsPrivateThreadPool(true);
        tcpInfo.setSupportsSpecifiedContentType(true);
        tcpInfo.setRequiresSpecifiedContentType(true);
        tcpInfo.setSupportsHardwiredServiceResolution(true);
        tcpInfo.setRequiresHardwiredServiceResolutionForNonXml(true);
        tcpInfo.setRequiresHardwiredServiceResolutionAlways(false);
        tcpInfo.setCustomPropertiesPanelClassname("com.l7tech.external.assertions.rawtcp.console.SimpleRawTransportPropertiesPanel");
        tcpInfo.setModularAssertionClassname(SimpleRawTransportAssertion.class.getName());
        ssgConnectorManager.registerTransportProtocol(tcpInfo, this);
    }

    @Override
    protected void doStop() throws LifecycleException {
        try {
            final List<Goid> oidsToStop = new ArrayList<Goid>(activeConnectors.keySet());
            for ( final Goid goid : oidsToStop) {
                removeConnector(goid);
            }
        }
        catch(Exception e) {
            auditError( SCHEME_RAW_TCP, "Error while shutting down.", e);
        }
    }

    @Override
    protected void doClose() throws LifecycleException {
        unregisterApplicationEventListenerAndCustomProtocols();
    }

    void unregisterApplicationEventListenerAndCustomProtocols() {
        for (String scheme : SUPPORTED_SCHEMES) {
            ssgConnectorManager.unregisterTransportProtocol(scheme);
        }
        applicationEventProxy.removeApplicationListener(this);
    }

    @Override
    protected boolean isCurrent( Goid goid, int version ) {
        boolean current;

        Pair<SsgConnector, ServerSock> entry = activeConnectors.get(goid);
        current = entry != null && entry.left.getVersion()==version;

        return current;
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        if ( connector.getGoid().equals( SsgConnector.DEFAULT_GOID ))
            throw new ListenerException("Connector must be persistent.");

        if (isCurrent(connector.getGoid(), connector.getVersion()))
            return;

        removeConnector(connector.getGoid());
        if (!connectorIsOwnedByThisModule(connector))
            return;

        connector = connector.getReadOnlyCopy();
        final String scheme = connector.getScheme();
        if (SCHEME_RAW_TCP.equalsIgnoreCase(scheme)) {
            addTcpConnector(connector);
        } else {
            // Can't happen
            logger.log(Level.WARNING, "ignoring connector with unrecognized scheme " + scheme);
        }
    }

    private void addTcpConnector(SsgConnector connector) throws ListenerException {
        if (!isLicensed())
            return;

        String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if (bindAddress == null || InetAddressUtil.isAnyHostAddress(bindAddress)) {
            bindAddress = InetAddressUtil.getAnyHostAddress();
        } else {
            bindAddress = ssgConnectorManager.translateBindAddress(bindAddress, connector.getPort());
        }

        int backlog = connector.getIntProperty(SimpleRawTransportAssertion.LISTEN_PROP_BACKLOG, 5);

        ExecutorService executor = requestExecutor;
        boolean executorNeedsClose = false;
        int poolSize = connector.getIntProperty(SsgConnector.PROP_THREAD_POOL_SIZE, 0);
        if (poolSize > 0) {
            executor = new ThreadPoolExecutor(poolSize, poolSize, KEEPALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            executorNeedsClose = true;
        }

        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName(bindAddress), connector.getPort()), backlog);
            final ServerSock serverSock = new ServerSock(connector, serverSocket, executor, executorNeedsClose);
            auditStart( SCHEME_RAW_TCP, describe(connector) );
            serverSock.start();
            activeConnectors.put(connector.getGoid(), new Pair<SsgConnector, ServerSock>(connector, serverSock));

        } catch (IOException e) {
            throw new ListenerException("Unable to create server socket: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void removeConnector(Goid goid) {
        final Pair<SsgConnector, ServerSock> entry;
        entry = activeConnectors.remove(goid);
        if (entry == null) return;
        ServerSock serverSock = entry.right;
        auditStop( SCHEME_RAW_TCP, describe( entry.left ) );
        try {
            if (serverSock != null) serverSock.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to close TCP socket: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected Set<String> getSupportedSchemes() {
        return SUPPORTED_SCHEMES;
    }

    private void handleRawTcpRequest(final Socket sock, SsgConnector connector) {
        Goid hardwiredServiceGoid = connector.getGoidProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, GoidEntity.DEFAULT_GOID);

        PolicyEnforcementContext context = null;
        InputStream responseStream = null;
        InetAddress address = sock.getInetAddress();
        HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connector.getGoid().toString() );
        HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.CLIENT_IP, address==null ? "" : address.getHostAddress() );
        try {

            Message request = new Message();
            Message response = new Message();
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            String ctypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
            final int readTimeout = connector.getIntProperty(SimpleRawTransportAssertion.LISTEN_PROP_READ_TIMEOUT, SimpleRawTransportAssertion.DEFAULT_READ_TIMEOUT);
            final long requestSizeLimit = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, SimpleRawTransportAssertion.DEFAULT_REQUEST_SIZE_LIMIT);

            sock.setSoTimeout(readTimeout);
            InputStream inputStream = new ByteLimitInputStream(sock.getInputStream(), 4096, requestSizeLimit);
            byte[] bytes = IOUtils.slurpStream(inputStream);

            ContentTypeHeader ctype = ctypeStr == null ? ContentTypeHeader.OCTET_STREAM_DEFAULT : ContentTypeHeader.create(ctypeStr);
            request.initialize(stashManagerFactory.createStashManager(), ctype, new ByteArrayInputStream(bytes), requestSizeLimit);
            request.attachKnob(TcpKnob.class, new SocketTcpKnob(sock));
            if (!Goid.isDefault(hardwiredServiceGoid)) {
                request.attachKnob(HasServiceGoid.class, new HasServiceGoidImpl(hardwiredServiceGoid));
            }

            AssertionStatus status = messageProcessor.processMessage(context);

            byte[] responseBytes = null;

            if (status != AssertionStatus.NONE) {
                // Send fault
                logger.log(Level.WARNING, "Raw TCP policy failed with assertion status: {0}", status);
                // TODO customize response to send upon error?
            } else if (response.getKnob(MimeKnob.class) != null && response.isInitialized()) {
                // Send response
                responseStream = response.getMimeKnob().getEntireMessageBodyAsInputStream();
                responseBytes = IOUtils.slurpStream(responseStream);
                // TODO response write timeout
                final int writeTimeout = connector.getIntProperty(SimpleRawTransportAssertion.LISTEN_PROP_WRITE_TIMEOUT, SimpleRawTransportAssertion.DEFAULT_WRITE_TIMEOUT);
                IOUtils.copyStream(new ByteArrayInputStream(responseBytes), sock.getOutputStream());
                sock.getOutputStream().flush();
            }

        } catch (IOException e) {
            logger.log(Level.WARNING, "I/O error handling raw TCP request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (Exception e) {
            // TODO send response of some kind?  customize response to send upon error?
            logger.log(Level.SEVERE, "Unexpected error handling raw TCP request: " + ExceptionUtils.getMessage(e), e);
        } finally {
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
            if (context != null)
                ResourceUtils.closeQuietly(context);
            ResourceUtils.closeQuietly(sock);
            if (responseStream != null)
                ResourceUtils.closeQuietly(responseStream);
        }
    }

}
