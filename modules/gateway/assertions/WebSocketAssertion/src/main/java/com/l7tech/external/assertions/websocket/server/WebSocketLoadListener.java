package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.GatewayState;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.transport.TransportAdminHelper;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cirving
 * Date: 5/29/12
 * Time: 4:18 PM
 */
public class WebSocketLoadListener {
    protected static final Logger logger = Logger.getLogger(WebSocketLoadListener.class.getName());

    private static MessageProcessor messageProcessor;
    private static Map<WebSocketConnectionEntity, Server> servers;
    private static ClusterPropertyManager clusterPropertyManager;
    private static SsgKeyStoreManager keyStoreManager;
    private static TrustManager trustManager;
    private static SecureRandom secureRandom;
    private static DefaultKey defaultKey;
    private static FirewallRulesManager fwManager;
    private static LicenseManager licenseManager;
    private static TransportAdminHelper transportAdminHelper;
    private static AuditFactory auditFactory;
    private static GenericEntityManager gem;
    private static ApplicationEventProxy applicationEventProxy;
    private static ApplicationListener applicationListener;

    private static boolean isStarted = false;

    /**
     * For outbound only connections, reconnect to WebSocket server if it's not connected.
     */
    private final static TimerTask reconnectTask = new TimerTask() {
        @Override
        public void run() {
            if (!isStarted) {
                return;
            }

            Collection<WebSocketConnectionEntity> entities;
            try {
                entities = gem.getEntityManager(WebSocketConnectionEntity.class).findAll();
            } catch (FindException e) {
                logger.log(Level.WARNING,
                        "Failed to retrieve WebSocket connection entities: " + ExceptionUtils.getMessageWithCause(e),
                        ExceptionUtils.getDebugException(e));
                return;
            }

            for (WebSocketConnectionEntity entity : entities) {
                if (!entity.isEnabled() || !entity.isOutboundOnly()) {
                    // Skip.
                    continue;
                }

                try {
                    // For outbound only connections, the handler ID is the entity ID.
                    final WebSocketOutboundHandler outboundHandler = WebSocketConnectionManager.getInstance().getOutboundHandler(entity.getId());
                    if (outboundHandler != null && !outboundHandler.existsInSocketMap(entity.getId())) {
                        logger.log(Level.INFO,
                                "Reconnecting WebSocket outbound handler ''{0}'' to WebSocket server.",
                                new String[]{ entity.getId() });
                        start(entity, true);
                    }
                } catch (WebSocketConnectionManagerException e) {
                    logger.log(Level.WARNING,
                            "Failed to retrieve WebSocket outbound handler ''" + entity.getId() + "'': " +  ExceptionUtils.getMessageWithCause(e),
                            ExceptionUtils.getDebugException(e));
                }
            }
        }
    };

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        // Register ExternalReferenceFactory
        messageProcessor = context.getBean("messageProcessor", MessageProcessor.class);
        clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        trustManager = context.getBean("routingTrustManager", TrustManager.class);
        secureRandom = context.getBean("secureRandom", SecureRandom.class);
        defaultKey = context.getBean("defaultKey", DefaultKey.class);
        fwManager = context.getBean("ssgFirewallManager", FirewallRulesManager.class);
        licenseManager = context.getBean("licenseManager", LicenseManager.class);
        final DefaultKey defaultKey = context.getBean("defaultKey", DefaultKey.class);
        transportAdminHelper = new TransportAdminHelper(defaultKey);
        auditFactory = context.getBean("auditFactory", AuditFactory.class);

        init(context);
        GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
        if( gatewayState.isReadyForMessages() ) {
            if (hasValidWebSocketLicense() && !isStarted) { // required for the case installing the .aar file
                                                            // on a live gateway or server module file.
                loadPropAndStartServer();
            }
        }
        // Only initialize all the WebSocket inbound/outbound resource managers when the SSG is "ready for messages"
        applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationListener = new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                if (event instanceof ReadyForMessages) {
                    if (hasValidWebSocketLicense() && !isStarted) {
                        loadPropAndStartServer();
                    }
                } else if (event instanceof EntityInvalidationEvent) {
                    if (event.getSource() instanceof GenericEntity) {
                        GenericEntity entity = (GenericEntity) event.getSource();
                        if (entity.getEntityClassName().equals(WebSocketConnectionEntity.class.getName())) {

                            final char[] ops = ((EntityInvalidationEvent) event).getEntityOperations();
                            final Goid[] goids = ((EntityInvalidationEvent) event).getEntityIds();
                            for (int i = 0; i < goids.length; ++i) {
                                if (ops[i] == EntityInvalidationEvent.CREATE) {
                                    logger.log(Level.INFO, "Created WebSocket Service " + entity.getId());
                                    try {
                                        start(WebSocketUtils.asConcreteEntity(entity, WebSocketConnectionEntity.class), true);
                                    } catch (FindException e) {
                                        logger.log(Level.WARNING, "Unable to find WebSocket Connection Entity");
                                    }
                                } else if (ops[i] == EntityInvalidationEvent.UPDATE) {
                                    logger.log(Level.INFO, "Changed WebSocket Service " + entity.getId());
                                    try {
                                        restart(WebSocketUtils.asConcreteEntity(entity, WebSocketConnectionEntity.class));
                                    } catch (FindException e) {
                                        logger.log(Level.WARNING, "Unable to find WebSocket Connection Entity");
                                    }
                                } else if (ops[i] == EntityInvalidationEvent.DELETE) {
                                    logger.log(Level.INFO, "Changed WebSocket Service " + entity.getId());
                                    try {
                                        WebSocketConnectionEntity wsEntity = WebSocketUtils.asConcreteEntity(entity, WebSocketConnectionEntity.class);
                                        wsEntity.setRemovePortFlag(true);
                                        stop(wsEntity);
                                    } catch (FindException e) {
                                        logger.log(Level.WARNING, "Unable to find WebSocket Connection Entity");
                                    }
                                }
                            }
                        }
                    }
                } else if (event instanceof LicenseEvent) {
                    if (hasValidWebSocketLicense() && !isStarted && gatewayState.isReadyForMessages()) {
                        init(context); // call init() because context can be destroyed when license removed.
                        loadPropAndStartServer();
                    } else if (!hasValidWebSocketLicense()) {
                        contextDestroyed();
                    }
                }
            }
        };

        applicationEventProxy.addApplicationListener(applicationListener);
    }

    private static void loadPropAndStartServer(){
        try {
            loadProps();
            contextInitialized();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to initialize WebSocket servers", e);
        }
    }

    private static boolean hasValidWebSocketLicense() {
        return licenseManager.isFeatureEnabled(GatewayFeatureSets.FS_WEBSOCKETS);
    }
    protected static int getIntegerProp (String key, int defaultValue) {
        return getProp(key, defaultValue, Integer::parseInt);
    }

    protected static int getTimeProp (String key, int defaultValue) {
        Number convertedValue = getProp(key, defaultValue, TimeUnit::parse);
        if (convertedValue.longValue() >= Integer.MAX_VALUE) {
            logger.log(Level.INFO, "Cluster property : '" + key + "' value is too long.  setting default value");
            return defaultValue;
        } else {
            return convertedValue.intValue();
        }
    }

    private static <T> T getProp(String key, T defaultValue, Function<String,T> valueTransformer ) {
        final String clusterProp;
        try {
            clusterProp = clusterPropertyManager.getProperty(key);
        } catch (FindException e) {
            logger.log(Level.INFO, "Cluster property : '" + key + "' doesn't exist setting default");
            return defaultValue;
        }
        if (clusterProp == null || "".equals(clusterProp)) {
            return defaultValue;
        }
        try {
            return valueTransformer.apply(clusterProp);
        } catch (Throwable t) {
            logger.log(Level.INFO, "Cluster property : '" + key + "' value is invalid: '" + clusterProp + "'. Using default value");
            return defaultValue;
        }
    }
    private static boolean getProp(String key, boolean defaultValue) {
        boolean prop;
        try {
            String clusterProp =  clusterPropertyManager.getProperty(key) ;
            if (clusterProp == null || "".equals(clusterProp)) {
                prop = defaultValue;
            } else {
                prop = Boolean.parseBoolean( clusterProp );
            }
        } catch(FindException e){
            logger.log(Level.INFO, "Cluster property : " + key + " doesn't exist setting default");
            prop = defaultValue;
        }
        return prop;
    }

    private static void loadProps() {
        WebSocketConstants.setClusterProperty(WebSocketConstants.CONNECT_TIMEOUT_KEY, getTimeProp(WebSocketConstants.CONNECT_TIMEOUT_KEY, WebSocketConstants.CONNECT_TIMEOUT));
        WebSocketConstants.setClusterProperty(WebSocketConstants.BUFFER_SIZE_KEY, getIntegerProp(WebSocketConstants.BUFFER_SIZE_KEY, WebSocketConstants.BUFFER_SIZE));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY, getIntegerProp(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY, WebSocketConstants.MAX_BINARY_MSG_SIZE));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY, getIntegerProp(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY, WebSocketConstants.MAX_TEXT_MSG_SIZE));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS_KEY, getTimeProp(WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS_KEY, WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS_KEY, getTimeProp(WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS_KEY, WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_CONNECTIONS_KEY, getIntegerProp(WebSocketConstants.MAX_INBOUND_CONNECTIONS_KEY, WebSocketConstants.MAX_INBOUND_CONNECTIONS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_OUTBOUND_THREADS_KEY, getIntegerProp(WebSocketConstants.MAX_OUTBOUND_THREADS_KEY, WebSocketConstants.MAX_OUTBOUND_THREADS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MIN_OUTBOUND_THREADS_KEY, getIntegerProp(WebSocketConstants.MIN_OUTBOUND_THREADS_KEY, WebSocketConstants.MIN_OUTBOUND_THREADS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_THREADS_KEY, getIntegerProp(WebSocketConstants.MAX_INBOUND_THREADS_KEY, WebSocketConstants.MAX_INBOUND_THREADS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MIN_INBOUND_THREADS_KEY, getIntegerProp(WebSocketConstants.MIN_INBOUND_THREADS_KEY, WebSocketConstants.MIN_INBOUND_THREADS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.ACCEPT_QUEUE_SIZE_KEY, getIntegerProp(WebSocketConstants.ACCEPT_QUEUE_SIZE_KEY, WebSocketConstants.ACCEPT_QUEUE_SIZE));
        WebSocketConstants.setClusterProperty(WebSocketConstants.OUTBOUND_ONLY_CONNECTION_RECONNECT_INTERVAL_KEY, getTimeProp(WebSocketConstants.OUTBOUND_ONLY_CONNECTION_RECONNECT_INTERVAL_KEY, WebSocketConstants.OUTBOUND_ONLY_CONNECTION_RECONNECT_INTERVAL));
        WebSocketConstants.setClusterProperty(WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY, getProp(WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY, WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER));
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded() {
        contextDestroyed();
        unregisterGenericEntities();
        applicationEventProxy.removeApplicationListener(applicationListener);
        applicationListener = null;
    }

    private static void init(ApplicationContext context) {
        servers = new ConcurrentHashMap<>();
        registerGenericEntities(context);
    }

    /**
     * Start Embedding Jetty server when WEB Application is started.
     */
    private static void contextInitialized() throws FindException {

        Collection<WebSocketConnectionEntity> connections = gem.getEntityManager(WebSocketConnectionEntity.class).findAll();
        try {
            WebSocketConnectionManager.createConnectionManager(keyStoreManager, trustManager, secureRandom, defaultKey);
            for (WebSocketConnectionEntity connection : connections) {
                start(connection, true);
            }
            isStarted = true;
        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.WARNING, "Failed to initialize WebSocket Connection Manager ", e);
        }

        Background.scheduleRepeated(
            reconnectTask,
            WebSocketConstants.getClusterProperty(WebSocketConstants.OUTBOUND_ONLY_CONNECTION_RECONNECT_INTERVAL_KEY),
            WebSocketConstants.getClusterProperty(WebSocketConstants.OUTBOUND_ONLY_CONNECTION_RECONNECT_INTERVAL_KEY));
    }



    /**
     * Stop Embedding Jetty server when WEB Application is stopped.
     */
    private static void contextDestroyed() {
        Background.cancel(reconnectTask);

        try {
            final Collection<WebSocketConnectionEntity> connections = gem.getEntityManager(WebSocketConnectionEntity.class).findAll();
            for (WebSocketConnectionEntity connection : connections) {
                connection.setRemovePortFlag(true);
                stop(connection);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING,
                    "Failed to retrieve WebSocket connection entities: " + ExceptionUtils.getMessageWithCause(e),
                    ExceptionUtils.getDebugException(e));
        } finally {
            isStarted = false;
        }
    }


    /**
     * register generic entities for this module
     */
    private static void registerGenericEntities(final ApplicationContext context) {

        if (gem == null) {
            gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        }

        if (gem.isRegistered(WebSocketConnectionEntity.class.getName())) {
            gem.unRegisterClass(WebSocketConnectionEntity.class.getName());
        }

        gem.registerClass(WebSocketConnectionEntity.class);
    }


    /**
     * Unregister generic entities for this module
     */
    private static void unregisterGenericEntities() {

        if (gem != null) {
            gem.unRegisterClass(WebSocketConnectionEntity.class.getName());
            gem = null;
        }
    }


    private static void restart(WebSocketConnectionEntity connection) {
        stop(connection);
        start(connection, false);
    }

    private static void start(WebSocketConnectionEntity connectionEntity, boolean forceFirewall) {

            if (connectionEntity.isEnabled()) {

                Server server = new Server(getInboundThreadPool());

                try {

                    if(!connectionEntity.isOutboundOnly()) {

                        if (connectionEntity.isInboundSsl()) {

                            SslContextFactory sslContextFactory = WebSocketConnectionManager.getInstance().getInboundSslCtxFactory(connectionEntity);

                            HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(new HttpConfiguration());
                            ServerConnector sslConnector = new ServerConnector(server, sslContextFactory, httpConnectionFactory);
                            sslConnector.setPort(connectionEntity.getInboundListenPort());
                            sslConnector.setAcceptQueueSize(WebSocketConstants.getClusterProperty(WebSocketConstants.ACCEPT_QUEUE_SIZE_KEY));
                            server.addConnector(sslConnector);

                        } else {
                            ServerConnector connector = new ServerConnector(server);
                            connector.setPort(connectionEntity.getInboundListenPort());
                            connector.setAcceptQueueSize(WebSocketConstants.getClusterProperty(WebSocketConstants.ACCEPT_QUEUE_SIZE_KEY));
                            server.addConnector(connector);
                        }

                        WebSocketInboundHandler webSocketInboundHandler = new WebSocketInboundHandler(messageProcessor, connectionEntity, auditFactory);
                        server.setHandler(webSocketInboundHandler);

                        //Register Inbound Handler
                        WebSocketConnectionManager.getInstance().registerInboundHandler(connectionEntity.getId(), webSocketInboundHandler);
                    }

                    if (!connectionEntity.isLoopback()) {
                        //Register outbound Handler
                        WebSocketOutboundHandler webSocketOutboundHandler = new WebSocketOutboundHandler(messageProcessor, connectionEntity);
                        WebSocketConnectionManager.getInstance().registerOutboundHandler(connectionEntity.getId(), webSocketOutboundHandler);

                        if(connectionEntity.isOutboundOnly()){
                            WebSocketMessage webSocketMessage = new WebSocketMessage("");
                            webSocketMessage.setOrigin("gw");
                            webSocketMessage.setProtocol("gw");
                            webSocketMessage.setId(connectionEntity.getId());
                            webSocketMessage.setClientId("gw");
                            MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
                            webSocketOutboundHandler.createConnection(connectionEntity.getOutboundUrl(), connectionEntity.getId(), webSocketMessage, mockHttpServletRequest);
                        }
                    }

                    if (!connectionEntity.isOutboundOnly() ) {
                        server.start();
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, server.dump());
                        }

                        StringBuilder statement = new StringBuilder();
                        if (connectionEntity.isInboundSsl()) {
                            statement.append("Starting WSS listener ");
                        } else {
                            statement.append("Starting WS listener ");
                        }
                        statement.append("on port ").append(connectionEntity.getInboundListenPort());

                        logger.log(Level.INFO, "Checking firewall state for port " + connectionEntity.getInboundListenPort() + " with connection state " + connectionEntity.getRemovePortFlag() + "," + forceFirewall);
                        //open the firewall for inbound port
                        if (connectionEntity.getRemovePortFlag() || forceFirewall) {
                            logger.log(Level.INFO, "Adding firewall rules for port " + connectionEntity.getInboundListenPort());
                            fwManager.openPort(connectionEntity.getId(), connectionEntity.getInboundListenPort());
                        }

                        logger.log(Level.INFO, statement.toString());

                        servers.put(connectionEntity, server);
                    }

                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Failed to start the WebSockets Server " + connectionEntity.getId(), e);
                    if (server.isFailed()){
                        try {
                            // DE250298-Inbound SSL - Selecting only SSLv2Hello throws Exception and Fails to start WebSocket Server.
                            server.stop();
                        } catch (Exception exception2) {
                            logger.log(Level.WARNING, "Failed to stop WebSocket Server:" + exception2.toString());
                        }
                    }
                }
            }
    }

    private static void stop(WebSocketConnectionEntity connectionEntity) {
        Server server = servers.remove(connectionEntity);
        if (server != null) {
            try {
                server.stop();
                //remove the port from firewall only if port has changed
                if (connectionEntity.getRemovePortFlag()) {
                    logger.log(Level.INFO, "Removing firewall rules for port " + connectionEntity.getInboundListenPort());
                    fwManager.removeRule(connectionEntity.getId());
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to stop the WebSockets Server", e);
            } finally {
                //Deregister handlers
                logger.log(Level.WARNING, "Deregistering handler : " + connectionEntity.getId());
                try {
                    WebSocketConnectionManager.getInstance().deregisterInboundHandler(connectionEntity.getId());
                    WebSocketConnectionManager.getInstance().deregisterOutboundHandler(connectionEntity.getId());
                } catch (WebSocketConnectionManagerException e) {
                    //Do nothing connection manager is already removed.
                    logger.log(Level.WARNING, "Caught exception when deregistering Inbound/Outbound handler " + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
                }
            }
        }

        if (server == null && connectionEntity.isOutboundOnly()) {
            //Deregister outbound handlers
            logger.log(Level.WARNING, "Deregistering outbound handler : " + connectionEntity.getId());
            try {
                WebSocketConnectionManager.getInstance().deregisterOutboundHandler(connectionEntity.getId());
            } catch (WebSocketConnectionManagerException e) {
                //Do nothing connection manager is already removed.
                logger.log(Level.WARNING, "Caught exception when deregistering Outbound handler " + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            }
        }
    }

    private static QueuedThreadPool getInboundThreadPool() {

        QueuedThreadPool inboundQueuedThreadPool = new QueuedThreadPool();

        int minInboundThreads = WebSocketConstants.getClusterProperty(WebSocketConstants.MIN_INBOUND_THREADS_KEY);
        if (minInboundThreads > 0) {
            inboundQueuedThreadPool.setMinThreads(minInboundThreads);
        }

        int maxInboundThreads = WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_INBOUND_THREADS_KEY);
        if (maxInboundThreads > 0) {
            inboundQueuedThreadPool.setMaxThreads(maxInboundThreads);
        }

        return inboundQueuedThreadPool;
    }

    @NotNull
    public static String[] getDefaultCipherSuiteNames() {
        return transportAdminHelper.getDefaultCipherSuiteNames();
    }
}
