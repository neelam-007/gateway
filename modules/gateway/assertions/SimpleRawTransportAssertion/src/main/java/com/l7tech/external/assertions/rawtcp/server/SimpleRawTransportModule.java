package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.message.HasServiceOid;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.FindException;
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
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SimpleRawTransportModule extends TransportModule implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(SimpleRawTransportModule.class.getName());
    private static final String SCHEME_RAW_TCP = "l7.raw.tcp";

    private static final Set<String> SUPPORTED_SCHEMES = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    static {
        SUPPORTED_SCHEMES.addAll(Arrays.asList(SCHEME_RAW_TCP));
    }

    private static BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<Runnable>();
    private static final int CORE_POOL_SIZE = 25;
    private static final int MAX_POOL_SIZE = 50;
    private static final long KEEPALIVE_SECONDS = 5 * 60;
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
    private final Object connectorCrudLuck = new Object();
    private final Map<Long, Pair<SsgConnector, ServerSock>> activeConnectors = new ConcurrentHashMap<Long, Pair<SsgConnector, ServerSock>>();

    public SimpleRawTransportModule(ApplicationEventProxy applicationEventProxy,
                                    LicenseManager licenseManager,
                                    SsgConnectorManager ssgConnectorManager,
                                    TrustedCertServices trustedCertServices,
                                    DefaultKey defaultKey,
                                    ServerConfig serverConfig,
                                    GatewayState gatewayState,
                                    MessageProcessor messageProcessor,
                                    StashManagerFactory stashManagerFactory)
    {
        super("Simple raw transport module", logger, null, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig);
        this.applicationEventProxy = applicationEventProxy;
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.stashManagerFactory = stashManagerFactory;
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        T got = beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass()))
            return got;
        throw new IllegalStateException("uanble to get get: " + beanName);

    }

    static SimpleRawTransportModule createModule(ApplicationContext appContext) {
        LicenseManager licenseManager = getBean(appContext, "licenseManager", LicenseManager.class);
        SsgConnectorManager ssgConnectorManager = getBean(appContext, "ssgConnectorManager", SsgConnectorManager.class);
        TrustedCertServices trustedCertServices = getBean(appContext, "trustedCertServices", TrustedCertServices.class);
        DefaultKey defaultKey = getBean(appContext, "defaultKey", DefaultKey.class);
        ServerConfig serverConfig = getBean(appContext, "serverConfig", ServerConfig.class);
        GatewayState gatewayState = getBean(appContext, "gatewayState", GatewayState.class);
        MessageProcessor messageProcessor = getBean(appContext, "messageProcessor", MessageProcessor.class);
        StashManagerFactory stashManagerFactory = getBean(appContext, "stashManagerFactory", StashManagerFactory.class);
        ApplicationEventProxy applicationEventProxy = getBean(appContext, "applicationEventProxy", ApplicationEventProxy.class);
        return new SimpleRawTransportModule(applicationEventProxy, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, serverConfig, gatewayState, messageProcessor, stashManagerFactory);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "Unable to access initial raw connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
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
                        if ( ExceptionUtils.getMessage(e).contains("java.net.BindException: ") ) { // The exception cause is not chained ...
                            logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                        ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        } else {
                            logger.log(Level.WARNING, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
                                        ": " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
            }

        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    @Override
    protected void doStart() throws LifecycleException {
        super.doStart();
        if (gatewayState.isReadyForMessages()) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "Unable to access initial raw connectors: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    void registerApplicationEventListenerAndCustomProtocols() {
        applicationEventProxy.addApplicationListener(this);

        TransportDescriptor tcpInfo = new TransportDescriptor();
        tcpInfo.setScheme(SCHEME_RAW_TCP);
        tcpInfo.setRequiresHardwiredServiceResolution(true);
        tcpInfo.setCustomPropertiesPanelClassname("com.l7tech.external.assertions.rawtcp.console.SimpleRawTransportPropertiesPanel");
        tcpInfo.setModularAssertionClassname(SimpleRawTransportAssertion.class.getName());
        ssgConnectorManager.registerTransportProtocol(tcpInfo, this);
    }

    @Override
    protected void doClose() throws LifecycleException {
        super.doClose();
        unregisterApplicationEventListenerAndCustomProtocols();
    }

    void unregisterApplicationEventListenerAndCustomProtocols() {
        for (String scheme : SUPPORTED_SCHEMES) {
            ssgConnectorManager.unregisterTransportProtocol(scheme);
        }
        applicationEventProxy.removeApplicationListener(this);
    }

    private boolean isCurrent( long oid, int version ) {
        boolean current;

        synchronized (connectorCrudLuck) {
            Pair<SsgConnector, ServerSock> entry = activeConnectors.get(oid);
            current = entry != null && entry.left.getVersion()==version;
        }

        return current;
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

            final String scheme = connector.getScheme();
            if (SCHEME_RAW_TCP.equalsIgnoreCase(scheme)) {
                addTcpConnector(connector);
            } else {
                // Can't happen
                logger.log(Level.WARNING, "ignoring connector with unrecognized scheme " + scheme);
            }
        }
    }

    private void addTcpConnector(SsgConnector connector) throws ListenerException {
        String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if (bindAddress != null && !bindAddress.equals("*") && !bindAddress.equals("0.0.0.0")) {
            bindAddress = ssgConnectorManager.translateBindAddress(bindAddress, connector.getPort());
        }

        int backlog = 5;
        String backlogVal = connector.getProperty("backlog");
        if (backlogVal != null && backlogVal.trim().length() > 0) {
            try {
                backlog = Integer.parseInt(backlogVal);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Ignoring invalid value for connector property backlog for listen port " + connector.getPort() + ": " + backlogVal);
            }
        }

        ExecutorService executor = requestExecutor;
        boolean executorNeedsClose = false;
        String poolSizeStr = connector.getProperty(SsgConnector.PROP_THREAD_POOL_SIZE);
        int poolSize = poolSizeStr != null ? Integer.parseInt(poolSizeStr) : 0;
        if (poolSize > 0) {
            executor = new ThreadPoolExecutor(poolSize, poolSize, KEEPALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            executorNeedsClose = true;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(connector.getPort(), backlog, InetAddress.getByName(bindAddress));
            final ServerSock serverSock = new ServerSock(connector, serverSocket, executor, executorNeedsClose);
            serverSock.start();
            activeConnectors.put(connector.getOid(), new Pair<SsgConnector, ServerSock>(connector, serverSock));

        } catch (IOException e) {
            throw new ListenerException("Unable to create server socket: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void removeConnector(long oid) {
        final Pair<SsgConnector, ServerSock> entry;
        synchronized (connectorCrudLuck) {
            entry = activeConnectors.remove(oid);
            if (entry == null) return;
            ServerSock serverSock = entry.right;
            logger.info("Removing " + entry.left.getScheme() + " connector on port " + entry.left.getPort());
            try {
                if (serverSock.tcpSocket != null) serverSock.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to close TCP socket: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    @Override
    protected Set<String> getSupportedSchemes() {
        return SUPPORTED_SCHEMES;
    }

    private void handleRawTcpRequest(Socket sock, SsgConnector connector) {
        long hardwiredServiceOid = -1;
        String oidStr = connector.getProperty("hardwired.service.id");
        if (oidStr != null && oidStr.trim().length() > 0) {
            hardwiredServiceOid = Long.parseLong(oidStr);
        }

        PolicyEnforcementContext context = null;
        InputStream responseStream = null;
        try {

            Message request = new Message();
            Message response = new Message();
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            String ctypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);

           // TODO configure SO_TIMEOUT, configure max request read time limit, configure max request size
            byte[] bytes = IOUtils.slurpStream(sock.getInputStream());

            ContentTypeHeader ctype = ctypeStr == null ? ContentTypeHeader.OCTET_STREAM_DEFAULT : ContentTypeHeader.parseValue(ctypeStr);
            request.initialize(stashManagerFactory.createStashManager(), ctype, new ByteArrayInputStream(bytes));
            if (hardwiredServiceOid != -1) {
                final long finalHardwiredServiceOid = hardwiredServiceOid;
                request.attachKnob(HasServiceOid.class, new HasServiceOid() {
                    @Override
                    public long getServiceOid() {
                        return finalHardwiredServiceOid;
                    }
                });
            }

            AssertionStatus status = messageProcessor.processMessage(context);

            if (response.getKnob(MimeKnob.class) != null) {
                // Send response
                responseStream = response.getMimeKnob().getEntireMessageBodyAsInputStream();
                byte[] responseBytes = IOUtils.slurpStream(responseStream);
                IOUtils.copyStream(new ByteArrayInputStream(responseBytes), sock.getOutputStream());
                sock.getOutputStream().flush();
            }


        } catch (IOException e) {
            logger.log(Level.WARNING, "I/O error handling raw TCP request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error handling raw TCP request: " + ExceptionUtils.getMessage(e), e);
        } finally {
            if (context != null)
                ResourceUtils.closeQuietly(context);
            ResourceUtils.closeQuietly(sock);
            if (responseStream != null)
                ResourceUtils.closeQuietly(responseStream);
        }
    }
}
