package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.ahttp.SubmitAsyncHttpResponseAssertion;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.message.HasServiceOid;
import com.l7tech.message.HasServiceOidImpl;
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
import com.l7tech.server.search.processors.DependencyProcessor;
import com.l7tech.server.search.processors.DoNothingDependencyProcessor;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.TransportModule;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.*;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.Component.GW_GENERIC_CONNECTOR;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;

/**
 *
 */
public class AsyncHttpTransportModule extends TransportModule implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(AsyncHttpTransportModule.class.getName());
    private static final long ASYNC_RESPONSE_TIMEOUT_MILLIS = SyspropUtil.getLong("com.l7tech.http.async.responseTimeout", 60L * 1000L);
    private static final int ASYNC_RESPONSE_TIMER_THREADS = Integer.getInteger("com.l7tech.http.async.responseTimeoutTimerThreads", 16);
    private static AsyncHttpTransportModule instance;

    private static Timer[] timerPool = new Timer[ASYNC_RESPONSE_TIMER_THREADS];
    static {
        for (int i = 0; i < timerPool.length; i++) {
            timerPool[i] = new Timer("Async HTTP request timeout thread #" + i, true);
        }
    }

    private static final String SCHEME_ASYNC_HTTP = "AHTTP";
    private static final Set<String> SUPPORTED_SCHEMES = caseInsensitiveSet(
        SCHEME_ASYNC_HTTP
    );

    private final ApplicationEventProxy applicationEventProxy;
    private final GatewayState gatewayState;
    private final MessageProcessor messageProcessor;
    private final StashManagerFactory stashManagerFactory;
    private final Config config;
    private final Map<Long, Pair<SsgConnector, AsyncHttpListenerInfo>> activeConnectors = new ConcurrentHashMap<Long, Pair<SsgConnector, AsyncHttpListenerInfo>>();

    private static final Map<String, PendingAsyncRequest> activeAsyncRequests = new ConcurrentHashMap<String, PendingAsyncRequest>(2048, 0.75f, 256);

    //This is the map of ssgConnectorDependency processors. We need it in order to add the AsyncHttp dependency processor
    private static Map<String, DependencyProcessor<SsgConnector>> ssgConnectorDependencyProcessorTypeMap;

    protected AsyncHttpTransportModule(@NotNull final ApplicationEventProxy applicationEventProxy,
                                       @NotNull final GatewayState gatewayState,
                                       @NotNull final MessageProcessor messageProcessor,
                                       @NotNull final StashManagerFactory stashManagerFactory,
                                       @Nullable final LicenseManager licenseManager,
                                       @NotNull final SsgConnectorManager ssgConnectorManager,
                                       @NotNull final TrustedCertServices trustedCertServices,
                                       @NotNull final DefaultKey defaultKey,
                                       @NotNull final Config config)
    {
        super("Simple raw transport module", GW_GENERIC_CONNECTOR, logger, GatewayFeatureSets.SERVICE_HTTP_MESSAGE_INPUT, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, config);
        this.applicationEventProxy = applicationEventProxy;
        this.gatewayState = gatewayState;
        this.messageProcessor = messageProcessor;
        this.stashManagerFactory = stashManagerFactory;
        this.config = config;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        super.onApplicationEvent(applicationEvent);

        if (applicationEvent instanceof ReadyForMessages) {
            try {
                startInitialConnectors();
            } catch (FindException e) {
                auditError( SCHEME_ASYNC_HTTP, "Unable to access initial async HTTP connectors: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
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
                        final Exception auditException;
                        if ( ExceptionUtils.getMessage(e).contains("java.net.BindException: ") ) { // The exception cause is not chained ...
                            auditException = ExceptionUtils.getDebugException(e);
                        } else {
                            auditException = e;
                        }
                        auditError( SCHEME_ASYNC_HTTP, "Unable to start " + connector.getScheme() + " connector on port " + connector.getPort() +
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
                auditError( SCHEME_ASYNC_HTTP, "Unable to access initial async HTTP connectors: " + ExceptionUtils.getMessage(e), e );
            }
        }
    }

    private void registerCustomProtocols() {
        TransportDescriptor tcpInfo = new TransportDescriptor();
        tcpInfo.setScheme(SCHEME_ASYNC_HTTP);
        tcpInfo.setSupportsPrivateThreadPool(false);
        tcpInfo.setSupportsSpecifiedContentType(true);
        tcpInfo.setRequiresSpecifiedContentType(false);
        tcpInfo.setSupportsHardwiredServiceResolution(true);
        tcpInfo.setRequiresHardwiredServiceResolutionForNonXml(false);
        tcpInfo.setRequiresHardwiredServiceResolutionAlways(false);
        tcpInfo.setModularAssertionClassname(SubmitAsyncHttpResponseAssertion.class.getName());
        ssgConnectorManager.registerTransportProtocol(tcpInfo, this);
    }

    @Override
    protected void doStop() throws LifecycleException {
        try {
            final List<Long> oidsToStop = new ArrayList<Long>(activeConnectors.keySet());
            for ( final Long oid : oidsToStop) {
                removeConnector(oid);
            }
        }
        catch (Exception e) {
            auditError( SCHEME_ASYNC_HTTP, "Error while shutting down.", e);
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
    protected boolean isCurrent( long oid, int version ) {
        boolean current;

        Pair<SsgConnector, AsyncHttpListenerInfo> entry = activeConnectors.get(oid);
        current = entry != null && entry.left.getVersion()==version;

        return current;
    }

    @Override
    protected void addConnector(SsgConnector connector) throws ListenerException {
        if ( connector.getOid() == SsgConnector.DEFAULT_OID )
            throw new ListenerException("Connector must be persistent.");

        if (isCurrent(connector.getOid(), connector.getVersion()))
            return;

        removeConnector(connector.getOid());
        if (!connectorIsOwnedByThisModule(connector))
            return;

        connector = connector.getReadOnlyCopy();
        final String scheme = connector.getScheme();
        if (SCHEME_ASYNC_HTTP.equalsIgnoreCase(scheme)) {
            addAsyncHttpConnector(connector);
        } else {
            // Can't happen
            logger.log(Level.WARNING, "ignoring connector with unrecognized scheme " + scheme);
        }
    }

    private void addAsyncHttpConnector(SsgConnector connector) throws ListenerException {
        if (!isLicensed())
            return;

        String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        if (bindAddress == null || InetAddressUtil.isAnyHostAddress(bindAddress)) {
            bindAddress = InetAddressUtil.getAnyHostAddress();
        } else {
            bindAddress = ssgConnectorManager.translateBindAddress(bindAddress, connector.getPort());
        }

        try {
            InetSocketAddress bindSockAddr = new InetSocketAddress(InetAddress.getByName(bindAddress), connector.getPort());
            final AsyncHttpListenerInfo listenerInfo = new AsyncHttpListenerInfo(this, connector, bindSockAddr);
            auditStart( SCHEME_ASYNC_HTTP, describe(connector) );
            listenerInfo.start();
            activeConnectors.put(connector.getOid(), new Pair<SsgConnector, AsyncHttpListenerInfo>(connector, listenerInfo));

        } catch (Exception e) {
            throw new ListenerException("Unable to create server socket: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void removeConnector(long oid) {
        final Pair<SsgConnector, AsyncHttpListenerInfo> entry;
        entry = activeConnectors.remove(oid);
        if (entry == null) return;
        AsyncHttpListenerInfo listenerInfo = entry.right;
        auditStop( SCHEME_ASYNC_HTTP, describe( entry.left ) );
        try {
            if (listenerInfo != null) listenerInfo.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to close TCP socket: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected Set<String> getSupportedSchemes() {
        return SUPPORTED_SCHEMES;
    }

    void submitRequestToMessageProcessor(@NotNull PendingAsyncRequest pendingRequest, @NotNull HttpRequest httpRequest, @NotNull HttpResponse httpResponse, @NotNull InputStream bodyInputStream, @Nullable InetSocketAddress clientAddress) {
        final AsyncHttpListenerInfo listenerInfo = pendingRequest.getListenerInfo();
        final SsgConnector connector = listenerInfo.getConnector();
        long hardwiredServiceOid = connector.getLongProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, -1L);

        String idToCleanup = null;
        PolicyEnforcementContext context = null;
        InputStream responseStream = null;
        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, Long.toString(connector.getOid()));
        HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.CLIENT_IP, clientAddress==null ? "" : clientAddress.getAddress().getHostAddress() );
        try {

            Message request = new Message();
            Message response = new Message();
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            String pinnedCtypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
            ContentTypeHeader pinnedCtype = pinnedCtypeStr == null ? null : ContentTypeHeader.create(pinnedCtypeStr);

            final ContentTypeHeader ctype;
            if (pinnedCtype != null) {
                // Hardwired override content type on this connector
                ctype = pinnedCtype;
            } else {
                String ctypeStr = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
                if (ctypeStr != null) {
                    ctype = ContentTypeHeader.parseValue(ctypeStr);
                } else {
                    ctype = ContentTypeHeader.OCTET_STREAM_DEFAULT;
                }
            }

            request.initialize(stashManagerFactory.createStashManager(), ctype, bodyInputStream);

            final InetSocketAddress serverAddress = listenerInfo.getBindAddress();
            URL requestUrl = new URL("http", serverAddress.getHostString(), serverAddress.getPort(), httpRequest.getUri());
            request.attachHttpRequestKnob(new NettyHttpRequestKnob(httpRequest, clientAddress, serverAddress, requestUrl));
            response.attachHttpResponseKnob(new NettyHttpResponseKnob(httpResponse));

            context.setVariable("ahttp.correlationId", pendingRequest.getCorrelationId());

            if (hardwiredServiceOid != -1L) {
                request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
            }

            final String correlationId = pendingRequest.getCorrelationId();
            idToCleanup = correlationId;
            activeAsyncRequests.put(correlationId, pendingRequest);

            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Registering pending async response with correlation ID " + pendingRequest.getCorrelationId());

            AssertionStatus status = messageProcessor.processMessage(context);

            if (status != AssertionStatus.NONE) {
                // Send fault
                logger.log(Level.WARNING, "Async HTTP policy failed with assertion status: {0}", status);
                // TODO customize response to send upon error?
                pendingRequest.errorAndClose(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Policy failed with assertion status: " + status);
            } else if (response.getKnob(MimeKnob.class) != null && response.isInitialized()) {
                // Send the response from this policy, don't bother waiting for an inbound policy
                logger.log(Level.FINE, "Async HTTP policy returned a synchronous response from the output policy -- returning it immediately");
                // TODO check if OK to do response streaming, when supported
                pendingRequest.respondAndMaybeClose(response, false);
            } else {
                // Response not initialized -- this is the common case.  Register an async request, set a timer task
                // to clean it up if it goes unclaimed for too long, and return without invoking the listener.
                idToCleanup = null;
                Timer timer = timerPool[Math.abs(request.hashCode()) % timerPool.length];
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            PendingAsyncRequest pending = activeAsyncRequests.remove(correlationId);
                            if (pending != null) {
                                // TODO customize response to send upon error?
                                pending.errorAndClose(HttpResponseStatus.GATEWAY_TIMEOUT, "Timeout awaiting async response");
                            }
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "Error in response timeout task: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                        }
                    }
                }, ASYNC_RESPONSE_TIMEOUT_MILLIS);
            }

        } catch (IOException e) {
            // TODO customize response to send upon error?
            final String msg = "I/O error handling async HTTP request: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            pendingRequest.errorAndClose(HttpResponseStatus.INTERNAL_SERVER_ERROR, msg);
        } catch (Exception e) {
            // TODO customize response to send upon error?
            final String msg = "Unexpected error handling async HTTP request: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            pendingRequest.errorAndClose(HttpResponseStatus.INTERNAL_SERVER_ERROR, msg);
        } finally {
            if (idToCleanup != null)
                activeAsyncRequests.remove(idToCleanup);

            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
            if (context != null)
                ResourceUtils.closeQuietly(context);
            if (responseStream != null)
                ResourceUtils.closeQuietly(responseStream);
        }
    }

    @Override
    public void reportMisconfiguredConnector(long connectorOid) {
        logger.log(Level.WARNING, "Shutting down async HTTP connector OID " + connectorOid + " because it cannot be opened in its current configuration");
        removeConnector(connectorOid);
    }

    static AsyncHttpTransportModule createModule( final ApplicationContext appContext ) {
        LicenseManager licenseManager = appContext.getBean("licenseManager", LicenseManager.class);
        SsgConnectorManager ssgConnectorManager = appContext.getBean("ssgConnectorManager", SsgConnectorManager.class);
        TrustedCertServices trustedCertServices = appContext.getBean("trustedCertServices", TrustedCertServices.class);
        DefaultKey defaultKey = appContext.getBean("defaultKey", DefaultKey.class);
        Config config = appContext.getBean("serverConfig", Config.class);
        GatewayState gatewayState = appContext.getBean("gatewayState", GatewayState.class);
        MessageProcessor messageProcessor = appContext.getBean("messageProcessor", MessageProcessor.class);
        StashManagerFactory stashManagerFactory = appContext.getBean("stashManagerFactory", StashManagerFactory.class);
        ApplicationEventProxy applicationEventProxy = appContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        final AsyncHttpTransportModule module = new AsyncHttpTransportModule(applicationEventProxy, gatewayState, messageProcessor, stashManagerFactory, licenseManager, ssgConnectorManager, trustedCertServices, defaultKey, config);
        module.setApplicationContext(appContext);
        applicationEventProxy.addApplicationListener(module);
        return module;
    }


    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "Async HTTP transport module is already initialized");
        } else {
            instance = createModule(context);
            try {
                instance.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "Async HTTP transport module threw exception on startup: " + ExceptionUtils.getMessage(e), e);
            }

            //get the map of SsgConnector dependency processors.
            //noinspection unchecked
            ssgConnectorDependencyProcessorTypeMap = context.getBean( "ssgConnectorDependencyProcessorTypeMap", Map.class );
            //add a custom processor for async http. this is the do nothing dependency processor because Async http does
            // not declare any dependencies beyond the default SsgConnector dependencies
            ssgConnectorDependencyProcessorTypeMap.put(SCHEME_ASYNC_HTTP, new DoNothingDependencyProcessor<SsgConnector>());
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "Async HTTP transport module is shutting down");
            try {
                instance.destroy();

            } catch (Exception e) {
                logger.log(Level.WARNING, "Async HTTP transport module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
        //remove the dependency processor
        ssgConnectorDependencyProcessorTypeMap.remove(SCHEME_ASYNC_HTTP);
    }

    public static boolean sendResponseToPendingRequest(String requestId, Message response, boolean destroyAsRead) {
        PendingAsyncRequest pending = activeAsyncRequests.remove(requestId);

        if (pending == null) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Unable to deliver async response -- no pending request active with ID: " + requestId);
            return false;
        }

        return pending.respondAndMaybeClose(response, destroyAsRead);
    }

}
