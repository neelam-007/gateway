package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.policy.module.AssertionModuleRegistrationEvent;
import com.l7tech.server.policy.module.ModularAssertionModule;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private static EntityManager<WebSocketConnectionEntity, GenericEntityHeader> entityManager;
    private static SsgKeyStoreManager keyStoreManager;
    private static TrustManager trustManager;
    private static SecureRandom secureRandom;
    private static DefaultKey defaultKey;
    private static FirewallRulesManager fwManager;

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
        init(context);
        // Only initialize all the XMPP inbound/outbound resource managers when the SSG is "ready for messages"
        ApplicationEventProxy applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {

                // only do this when the SSG is ready for messages
                if (event instanceof AssertionModuleRegistrationEvent ) {
                    logger.log(Level.INFO, "Starting WebSocket Services");
                    AssertionModuleRegistrationEvent registrationEvent = (AssertionModuleRegistrationEvent)event;
                    Set<? extends Assertion> protos = ((ModularAssertionModule)registrationEvent.getModule()).getAssertionPrototypes();
                    if (protos.size() > 0) {
                        Assertion proto = protos.iterator().next();
                        if (proto.getClass().getClassLoader() == getClass().getClassLoader()) {
                            // Our module has just been registered.  Time to do our delayed initialization.
                            try {
                                loadProps();
                                contextInitialized();
                            } catch (FindException e) {
                                logger.log(Level.WARNING, "Unable to initialize WebSocket servers", e);
                            }
                        }
                    }
                } else if (event instanceof Created) {
                    if (((Created) event).getEntity() instanceof GenericEntity) {
                        GenericEntity entity = (GenericEntity) ((Created) event).getEntity();
                        if (entity.getEntityClassName().equals(WebSocketConnectionEntity.class.getName())) {
                            logger.log(Level.INFO, "Created WebSocket Service " + entity.getId());
                            try {
                                start(WebSocketUtils.asConcreteEntity(entity, WebSocketConnectionEntity.class));
                            } catch (FindException e) {
                               logger.log(Level.WARNING, "Unable to find WebSocket Connection Entity");
                            }
                        }
                    }
                } else {
                    if (event instanceof EntityInvalidationEvent) {
                        if (event.getSource() instanceof GenericEntity) {
                            GenericEntity entity = (GenericEntity) event.getSource();
                            if (entity.getEntityClassName().equals(WebSocketConnectionEntity.class.getName())) {
                                if (((EntityInvalidationEvent) event).getEntityOperations()[0] == 'U') {
                                    logger.log(Level.INFO, "Changed WebSocket Service " + entity.getId());
                                    try {
                                        restart(WebSocketUtils.asConcreteEntity(entity, WebSocketConnectionEntity.class));
                                    } catch (FindException e) {
                                        logger.log(Level.WARNING, "Unable to find WebSocket Connection Entity");
                                    }
                                }
                            }
                        }
                    } else if (event instanceof Deleted) {
                        if (((Deleted) event).getEntity() instanceof GenericEntity) {
                            GenericEntity entity = (GenericEntity) ((Deleted) event).getEntity();
                            if (entity.getEntityClassName().equals(WebSocketConnectionEntity.class.getName())) {
                                logger.log(Level.INFO, "Changed WebSocket Service " + entity.getId());
                                Set<WebSocketConnectionEntity> connections = servers.keySet();

                                for (WebSocketConnectionEntity connection : connections) {
                                    if (connection.getId().equals(entity.getId())) {
                                        connection.setRemovePortFlag(true);
                                        stop(connection);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

    }

    private static int getProp(String key, int defaultValue) {
         int prop;
         try {
            String clusterProp = clusterPropertyManager.getProperty(key);
            if (clusterProp == null || "".equals(clusterProp)) {
                prop = defaultValue;
            } else {
                prop = Integer.parseInt(clusterProp);
            }
        } catch (FindException e) {
            logger.log(Level.INFO, "Cluster property : " + key + " doesn't exist setting default");
            prop = defaultValue;
        }
        return prop;
    }

    private static void loadProps() {
        WebSocketConstants.setClusterProperty(WebSocketConstants.CONNECT_TIMEOUT_KEY, getProp(WebSocketConstants.CONNECT_TIMEOUT_KEY, WebSocketConstants.CONNECT_TIMEOUT));
        WebSocketConstants.setClusterProperty(WebSocketConstants.BUFFER_SIZE_KEY, getProp(WebSocketConstants.BUFFER_SIZE_KEY, WebSocketConstants.BUFFER_SIZE));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY, getProp(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY, WebSocketConstants.MAX_BINARY_MSG_SIZE));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY, getProp(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY, WebSocketConstants.MAX_TEXT_MSG_SIZE));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS_KEY, getProp(WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS_KEY, WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS_KEY, getProp(WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS_KEY, WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_CONNECTIONS_KEY, getProp(WebSocketConstants.MAX_INBOUND_CONNECTIONS_KEY, WebSocketConstants.MAX_INBOUND_CONNECTIONS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_OUTBOUND_THREADS_KEY, getProp(WebSocketConstants.MAX_OUTBOUND_THREADS_KEY, WebSocketConstants.MAX_OUTBOUND_THREADS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MIN_OUTBOUND_THREADS_KEY, getProp(WebSocketConstants.MIN_OUTBOUND_THREADS_KEY, WebSocketConstants.MIN_OUTBOUND_THREADS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MAX_INBOUND_THREADS_KEY, getProp(WebSocketConstants.MAX_INBOUND_THREADS_KEY, WebSocketConstants.MAX_INBOUND_THREADS));
        WebSocketConstants.setClusterProperty(WebSocketConstants.MIN_INBOUND_THREADS_KEY, getProp(WebSocketConstants.MIN_INBOUND_THREADS_KEY, WebSocketConstants.MIN_INBOUND_THREADS));
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded() {
        contextDestroyed();
    }

    private static void init(ApplicationContext context) {
        servers = new ConcurrentHashMap<>();

        entityManager = EntityManagerFactory.getEntityManager(context);
    }

    /**
     * Start Embedding Jetty server when WEB Application is started.
     */
    private static void contextInitialized() throws FindException {

        Collection<WebSocketConnectionEntity> connections = entityManager.findAll();
        try {
            WebSocketConnectionManager.createConnectionManager(keyStoreManager,trustManager,secureRandom,defaultKey);
            for (WebSocketConnectionEntity connection : connections) {
                start(connection);
            }
        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.WARNING, "Failed to initialize WebSocket Connection Manager ", e);
        }
    }

    /**
     * Stop Embedding Jetty server when WEB Application is stopped.
     */
    private static void contextDestroyed() {
        Set<WebSocketConnectionEntity> connections = servers.keySet();

        for (WebSocketConnectionEntity connection : connections) {
          stop(connection);
        }
    }

    private static void restart(WebSocketConnectionEntity connection) {
        stop(connection);
        start(connection);
    }

    private static void start(WebSocketConnectionEntity connection) {
        try {
            if (connection.isEnabled()) {
                Server server = new Server(connection.getInboundListenPort());
                if (!connection.isInboundSsl()) {
                    server.setThreadPool( WebSocketConnectionManager.getInstance().getInboundThreadPool());
                }

                WebSocketInboundHandler webSocketInboundHandler = new WebSocketInboundHandler(messageProcessor, connection);
                webSocketInboundHandler.setHandler(new DefaultHandler());
                server.setHandler(webSocketInboundHandler);

                //Register Inbound Handler
                WebSocketConnectionManager.getInstance().registerInboundHandler(connection.getId(), webSocketInboundHandler);

                if (!connection.isLoopback()) {
                    //Register outbound Handler
                    WebSocketOutboundHandler webSocketOutboundHandler = new WebSocketOutboundHandler(messageProcessor, connection);
                    WebSocketConnectionManager.getInstance().registerOutboundHandler(connection.getId(), webSocketOutboundHandler,connection);
                }

                if (connection.isInboundSsl()) {
                    SslSelectChannelConnector sslSocketConnector = WebSocketConnectionManager.getInstance().getInboundSSLConnector(connection);
                    sslSocketConnector.setPort(connection.getInboundListenPort());
                    server.setConnectors(new Connector[] { sslSocketConnector });
                }

                server.start();
                StringBuilder statement = new StringBuilder();
                if ( connection.isInboundSsl() ) {
                    statement.append("Starting WSS listener ");
                } else {
                    statement.append("Starting WS listener ");
                }
                statement.append("on port ").append(connection.getInboundListenPort());
                //open the firewall for inbound port
                if (connection.getRemovePortFlag()) {
                    fwManager.openPort(connection.getId(), connection.getInboundListenPort());
                }

                logger.log(Level.INFO, statement.toString());

                servers.put(connection, server);
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Failed to start the WebSockets Server " + connection.getId(), e);
        }
    }

    private static void stop(WebSocketConnectionEntity connection) {
        Server server = servers.remove(connection);
        if ( server != null ) {
            try {
                server.stop();
                //remove the port from firewall only if port has changed
                if (connection.getRemovePortFlag()){
                    fwManager.removeRule(connection.getId());
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to stop the WebSockets Server", e);
            } finally {
                //Deregister handlers
                logger.log(Level.WARNING, "Deregistering handler : " + connection.getId());
                try {
                    WebSocketConnectionManager.getInstance().deregisterInboundHandler(connection.getId());
                    WebSocketConnectionManager.getInstance().deregisterOutboundHandler(connection.getId());
                } catch (WebSocketConnectionManagerException e) {
                    //Do nothing connection manager is already removed.
                }

            }
        }
    }
}
