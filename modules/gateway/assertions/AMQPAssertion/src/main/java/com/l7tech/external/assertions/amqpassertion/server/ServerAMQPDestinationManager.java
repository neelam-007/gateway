package com.l7tech.external.assertions.amqpassertion.server;

import com.l7tech.common.io.SingleCertX509KeyManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpSsgActiveConnector;
import com.l7tech.external.assertions.amqpassertion.console.AMQPDestinationHelper;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.message.Header;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.event.ServiceEnablementEvent;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.TimeUnit;
import com.rabbitmq.client.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.security.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/8/12
 * Time: 9:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerAMQPDestinationManager implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(ServerAMQPDestinationManager.class.getName());

    private static ServerAMQPDestinationManager INSTANCE;

    private ServiceCache serviceCache;

    private ClusterPropertyManager clusterPropertyManager;
    private SsgKeyStoreManager keyStoreManager;
    private DefaultKey defaultKey;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private StashManagerFactory stashManagerFactory;
    private MessageProcessor messageProcessor;
    private SecurePasswordManager securePasswordManager;
    protected SsgActiveConnectorManager ssgActiveConnectorManager;
    private String clusterNodeId;
    private final ApplicationEventPublisher messageProcessingEventChannel;
    private AtomicInteger nodeMessageId = new AtomicInteger(1);
    protected HashMap<Goid, AMQPDestination> destinations = new HashMap<Goid, AMQPDestination>();

    protected HashMap<Goid, Channel> serverChannels = new HashMap<Goid, Channel>();
    protected HashMap<Goid, CachedChannel> clientChannels = new HashMap<Goid, CachedChannel>();
    protected HashMap<Goid, Long> failedConsumers = new HashMap<Goid, Long>();
    protected HashMap<Goid, Long> failedProducers = new HashMap<Goid, Long>();

    private Timer failedConsumersTimer;
    private Timer clientChannelEvictionTimer;

    protected long responseTimeout = ModuleLoadListener.DEFAULT_AMQP_RESPONSE_TIMEOUT_MS;
    protected int maxMessageSize = ModuleLoadListener.DEFAULT_AMQP_MESSAGE_MAX_BYTES;
    protected long inboundConnectTimeout = ModuleLoadListener.DEFAULT_AMQP_CONNECT_ERROR_SLEEP_MS;
    protected long connectionMaxAge = TimeUnit.parse(ModuleLoadListener.DEFAULT_AMQP_CONNECTION_CACHE_MAX_AGE_TIMEUNIT);
    protected long connectionMaxIdle = TimeUnit.parse(ModuleLoadListener.DEFAULT_AMQP_CONNECTION_CACHE_MAX_IDLE_TIMEUNIT);

    private Config config;
    private final static AtomicReference<AMQPDestinationManagerConfig> cacheConfigReference = new AtomicReference<AMQPDestinationManagerConfig>(new AMQPDestinationManagerConfig(0, 0));

    static void setInstance(ServerAMQPDestinationManager instance) {
        INSTANCE = instance;
    }

    public static ServerAMQPDestinationManager getInstance(ApplicationContext context) {
        if (INSTANCE == null) {
            ServerConfig config = context.getBean("serverConfig", ServerConfig.class);
            INSTANCE = new ServerAMQPDestinationManager(config, context);
        }

        return INSTANCE;
    }

    protected ServerAMQPDestinationManager(Config config, ApplicationContext context) {

        messageProcessor = context.getBean("messageProcessor", MessageProcessor.class);
        secureRandom = context.getBean("secureRandom", SecureRandom.class);
        keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        stashManagerFactory = context.getBean("stashManagerFactory", StashManagerFactory.class);
        clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        defaultKey = context.getBean("defaultKey", DefaultKey.class);
        ssgActiveConnectorManager = context.getBean("ssgActiveConnectorManager", SsgActiveConnectorManager.class);
        securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
        clusterNodeId = (String) context.getBean("clusterNodeId");
        messageProcessingEventChannel = context.getBean("messageProcessingEventChannel", EventChannel.class);
        trustManager = context.getBean("trustManager", TrustManager.class);
        serviceCache = context.getBean("serviceCache", ServiceCache.class);
        this.config = config;
    }

    public void start() {
        loadClusterProperties();
        loadDestinations();
        // Every 30 seconds check for consumers that failed to connect
        failedConsumersTimer = new Timer();
        failedConsumersTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<AMQPDestination> failedDestinations = new ArrayList<AMQPDestination>();
                synchronized (failedConsumers) {
                    for (Iterator<Map.Entry<Goid, Long>> it = failedConsumers.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry<Goid, Long> entry = it.next();

                        if (entry.getValue() < System.currentTimeMillis() - inboundConnectTimeout) {
                            failedDestinations.add(destinations.get(entry.getKey()));
                            it.remove();
                        }
                    }
                }

                for (AMQPDestination destination : failedDestinations) {
                    activateConsumerDestination(destination);
                }
            }
        }, inboundConnectTimeout, inboundConnectTimeout);

        clientChannelEvictionTimer = new Timer();
        clientChannelEvictionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (clientChannels) {
                    for (Iterator<Map.Entry<Goid, CachedChannel>> it = clientChannels.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry<Goid, CachedChannel> entry = it.next();

                        if (entry.getValue().isExpired()) {
                            try {
                                Channel channel = entry.getValue().getClientChannel();
                                Connection connection = entry.getValue().getClientChannel().getConnection();
                                channel.close();
                                connection.close();
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Failed to close the AMQP destination: " + entry.getKey(), e);
                            } catch (AlreadyClosedException e) {
                                // Connection already closed.
                                logger.log(Level.FINE, "Attempting to close already closed AMQP destination: " + entry.getKey(), e);
                            }

                            //Setting the producer to automatically re-connect the connection, as this was not a "failed" connection
                            //in the strict sense. This is a cache expiry
                            failedProducers.put(entry.getKey(), System.currentTimeMillis() - ModuleLoadListener.DEFAULT_AMQP_CONNECT_ERROR_SLEEP_MS);
                            it.remove();
                        }
                    }
                }
            }
        }, 30000L, 30000L);
    }

    /**
     * Retrieves a map of active destination objects to their active server channels.
     *
     * @return a map of destinations mapped to active server channels
     */
    public HashMap<Goid, Channel> getServerChannels() {
        return serverChannels;
    }

    /**
     * Retrieves a map of active destination objects and the time at which they failed to connect.
     *
     * @return map of destinations that failed to connect and when they failed to connect
     */
    public HashMap<Goid, Long> getFailedConsumers() {
        return failedConsumers;
    }

    protected void loadClusterProperties() {
        try {
            String val = clusterPropertyManager.getProperty(ModuleLoadListener.AMQP_RESPONSE_TIMEOUT_UI_PROPERTY);
            if (val != null) {
                responseTimeout = Long.parseLong(val);
            } else {
                responseTimeout = ModuleLoadListener.DEFAULT_AMQP_RESPONSE_TIMEOUT_MS;
            }
        } catch (NumberFormatException | FindException e) {
            logger.log(Level.WARNING, "Error loading cluster-wide property " + ModuleLoadListener.AMQP_RESPONSE_TIMEOUT_UI_PROPERTY + ". Using default value.", e);
            responseTimeout = ModuleLoadListener.DEFAULT_AMQP_RESPONSE_TIMEOUT_MS;
        }

        try {
            String val = clusterPropertyManager.getProperty(ModuleLoadListener.AMQP_MESSAGE_MAX_BYTES_UI_PROPERTY);
            if (val != null) {
                maxMessageSize = Integer.parseInt(val);
            } else {
                maxMessageSize = ModuleLoadListener.DEFAULT_AMQP_MESSAGE_MAX_BYTES;
            }
        } catch (NumberFormatException | FindException e) {
            logger.log(Level.WARNING, "Error loading cluster-wide property " + ModuleLoadListener.AMQP_MESSAGE_MAX_BYTES_UI_PROPERTY + ". Using default value.", e);
            maxMessageSize = ModuleLoadListener.DEFAULT_AMQP_MESSAGE_MAX_BYTES;
        }

        try {
            String val = clusterPropertyManager.getProperty(ModuleLoadListener.AMQP_CONNECT_ERROR_SLEEP_UI_PROPERTY);
            if (val != null) {
                inboundConnectTimeout = Long.parseLong(val);
            } else {
                inboundConnectTimeout = ModuleLoadListener.DEFAULT_AMQP_CONNECT_ERROR_SLEEP_MS;
            }
        } catch (NumberFormatException | FindException e) {
            logger.log(Level.WARNING, "Error loading cluster-wide property " + ModuleLoadListener.AMQP_CONNECT_ERROR_SLEEP_UI_PROPERTY + ". Using default value.", e);
            inboundConnectTimeout = ModuleLoadListener.DEFAULT_AMQP_CONNECT_ERROR_SLEEP_MS;
        }

        logger.config("Loading cache channel configuration.");

        final long maximumAge = config.getTimeUnitProperty(ModuleLoadListener.AMQP_CONNECTION_CACHE_MAX_AGE_PROPERTY, connectionMaxAge);
        final long maximumIdleTime = config.getTimeUnitProperty(ModuleLoadListener.AMQP_CONNECTION_CACHE_MAX_IDLE_PROPERTY, connectionMaxIdle);

        cacheConfigReference.set(new AMQPDestinationManagerConfig(
                        rangeValidate(maximumAge, connectionMaxAge, 0L, Long.MAX_VALUE, "AMQP Connection Maximum Age"),
                        rangeValidate(maximumIdleTime, connectionMaxIdle, 0L, Long.MAX_VALUE, "AMQP Connection Maximum Idle"))
        );
    }


    protected AMQP.BasicProperties.Builder createNewPropertiesBuilder(HashMap props) {
        AMQP.BasicProperties.Builder properties = new AMQP.BasicProperties.Builder();
        Integer deliveryMode = (Integer) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.DELIVERY_MODE);
        if (null != deliveryMode) {
            properties.deliveryMode(deliveryMode);
        }
        String encoding = (String) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.CONTENT_ENCODING);
        if (null != encoding) {
            properties.contentEncoding(encoding);
        }
        Integer priority = (Integer) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.PRIORITY);
        if (null != priority) {
            properties.priority(priority);
        }
        String expiration = (String) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.EXPIRATION);
        if (null != expiration) {
            properties.expiration(expiration);
        }
        String type = (String) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.TYPE);
        if (null != type) {
            properties.type(type);
        }
        String userId = (String) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.USER_ID);
        if (null != userId) {
            properties.userId(userId);
        }
        String appId = (String) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.APP_ID);
        if (null != appId) {
            properties.appId(appId);
        }
        String clusterId = (String) props.get(ServerRouteViaAMQPAssertion.AMQP_PREFIX + "." + ServerRouteViaAMQPAssertion.CLUSTER_ID);
        if (null != clusterId) {
            properties.clusterId(clusterId);
        }
        return properties;
    }

    protected void loadDestinations() {
        updateAndValidateDestinations();
    }

    public void shutdown() {
        failedConsumersTimer.cancel();

        for (AMQPDestination destination : destinations.values()) {
            deactivateDestination(destination);
        }
    }

    private void activateConsumerDestination(AMQPDestination destination) {
        destinations.put(destination.getGoid(), destination);
        if (serverChannels.containsKey(destination.getGoid())) {
            return;
        }

        try {
            Channel channel = activateConsumerDestinationHelper(destination);
            if (channel != null) {
                AMQPConsumer consumer = new AMQPConsumer(channel, destination, stashManagerFactory,
                        messageProcessor, messageProcessingEventChannel, this);
                channel.basicConsume(destination.getQueueName(),
                        JmsAcknowledgementType.AUTOMATIC == destination.getAcknowledgementType(), consumer);

                serverChannels.put(destination.getGoid(), channel);
            } else {
                synchronized (failedConsumers) {
                    failedConsumers.put(destination.getGoid(), System.currentTimeMillis());
                }
            }
        } catch (FindException | ParseException | IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            synchronized (failedConsumers) {
                failedConsumers.put(destination.getGoid(), System.currentTimeMillis());
            }
        }
    }

    private void activateProducerDestination(AMQPDestination destination) {
        destinations.put(destination.getGoid(), destination);

        if (clientChannels.containsKey(destination.getGoid())) {
            return;
        }
        try {
            Channel channel = activateProducerDestinationHelper(destination);
            if (channel != null) {
                clientChannels.put(destination.getGoid(), new CachedChannel(channel));
            } else {
                synchronized (failedProducers) {
                    failedProducers.put(destination.getGoid(), System.currentTimeMillis());
                }
            }
        } catch (FindException | ParseException | IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            synchronized (failedProducers) {
                failedProducers.put(destination.getGoid(), System.currentTimeMillis());
            }
        }
    }

    private void deactivateDestination(AMQPDestination destination) {
        Channel channel = null;
        if (destination.isInbound()) {
            channel = serverChannels.remove(destination.getGoid());
        } else if (destination.getGoid() != null) {
            CachedChannel cachedChannel = clientChannels.remove(destination.getGoid());
            if (cachedChannel != null) {
                channel = cachedChannel.getClientChannel();
            }
        }

        if (channel != null) {
            try {
                Connection connection = channel.getConnection();
                channel.close();
                connection.close();
                logger.log(Level.INFO, "Closed AMQP Destination: " + destination.getName());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to close the AMQP destination: " + destination.getName(), e);
            } catch (AlreadyClosedException e) {
                // Connection already closed.
                logger.log(Level.FINE, "Attempting to close already closed AMQP destination: " + destination.getName(), e);
            }
        }
    }

    public boolean queueMessage(Goid destinationGoid, String routingKey, Message requestMsg, Message responseMsg, HashMap amqpProps) {
        return reallyQueueMessage(destinationGoid, routingKey, requestMsg, responseMsg, amqpProps);
    }

    protected boolean reallyQueueMessage(Goid destinationGoid, String routingKey, Message requestMsg, Message responseMsg, HashMap amqpProps) {
        if (maxMessageSize > 0) {
            try {
                if (maxMessageSize < requestMsg.getMimeKnob().getContentLength()) {
                    logger.log(Level.WARNING, "The request message was larger than the max AMQP message size limit.");
                    return false;
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to check the size of the request message.");
                return false;
            }
        }


        AMQPDestination destination = destinations.get(destinationGoid);


        if (destination != null && (!clientChannels.containsKey(destinationGoid) || !clientChannels.get(destinationGoid).getClientChannel().isOpen())) {

            if (clientChannels.containsKey(destinationGoid) && !clientChannels.get(destinationGoid).getClientChannel().isOpen()) {
                clientChannels.remove(destinationGoid);
                failedProducers.put(destinationGoid, System.currentTimeMillis());
            }

            // Try to reconnect producer
            boolean reconnect = false;
            synchronized (failedProducers) {
                if (failedProducers.containsKey(destinationGoid) &&
                        failedProducers.get(destinationGoid) < System.currentTimeMillis() - inboundConnectTimeout) {
                    reconnect = true;
                    failedProducers.remove(destinationGoid);
                }
            }

            if (reconnect) {
                activateProducerDestination(destination);
            }
        }

        boolean result = false;
        boolean cacheChannelIncremented = false;
        CachedChannel cachedChannel = clientChannels.get(destinationGoid);

        if (destination != null && clientChannels.containsKey(destinationGoid) && cachedChannel.getClientChannel().isOpen()) {
            cachedChannel.incrementRef();
            cacheChannelIncremented = true;
            Channel channel = cachedChannel.getClientChannel();

            if (AMQPDestination.OutboundReplyBehaviour.TEMPORARY_QUEUE == destination.getOutboundReplyBehaviour()) {
                try {
                    String queueName = channel.queueDeclare("", false, true, true, null).getQueue();

                    result = queueMessageWaitForResponse(channel, queueName, destination, routingKey, requestMsg, responseMsg, amqpProps);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to create the temporary response queue.", e);
                } catch (ShutdownSignalException e) {
                    logger.log(Level.WARNING, "Failed to create the temporary response queue.", e);
                }
            } else if (AMQPDestination.OutboundReplyBehaviour.ONE_WAY == destination.getOutboundReplyBehaviour()) {
                result = queueOneWayMessage(channel, destination, routingKey, requestMsg, amqpProps);
            } else if (AMQPDestination.OutboundReplyBehaviour.SPECIFIED_QUEUE == destination.getOutboundReplyBehaviour()) {
                result = queueMessageWaitForResponse(channel, destination.getResponseQueue(),
                        destination, routingKey, requestMsg, responseMsg, amqpProps);
            } else {
                logger.log(Level.WARNING, "Unknown outbound reply type (" + destination.getOutboundReplyBehaviour() + ").");
            }
        } else if (destination == null) {
            logger.log(Level.WARNING, "Unknown AMQP destination.");
        } else if (!clientChannels.containsKey(destinationGoid)) {
            logger.log(Level.WARNING, "The required AMQP destination is not connected.");
        } else {
            logger.log(Level.WARNING, "The required AMQP destination connection has been lost.");
        }

        // The cache channel has been reference count incremented,
        // now after the receiving of message we can decrement the reference
        // count
        if (cacheChannelIncremented) {
            cachedChannel.decrementRef();
        }

        return result;
    }

    private boolean queueOneWayMessage(Channel channel, AMQPDestination destination, String routingKey, Message message,
                                       HashMap amqpProperties) {
        try {

            AMQP.BasicProperties.Builder propertiesBuilder = createNewPropertiesBuilder(amqpProperties);

            final HeadersKnob amqpHeadersKnob = message.getKnob(HeadersKnob.class);

            Map<String, Object> messageHeaders = new HashMap<String, Object>();

            if (amqpHeadersKnob != null) {
                Collection<Header> headers = amqpHeadersKnob.getHeaders();

                HashMap<String, Object> propertyMap = new HashMap<>();

                for (Header header : headers) {
                    propertyMap.put(header.getKey(), header.getValue());
                }

                for (String key : propertyMap.keySet()) {
                    messageHeaders.put(key, propertyMap.get(key));
                }
                propertiesBuilder.headers(messageHeaders);
            }

            propertiesBuilder.contentType(message.getMimeKnob().getOuterContentType().getFullValue()).
                    messageId(clusterNodeId + "-" + nodeMessageId.getAndIncrement()).
                    timestamp(new Date());
            channel.basicPublish(destination.getExchangeName(),
                    routingKey,
                    propertiesBuilder.build(),
                    IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()));

            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to queue the request message.", e);
            return false;
        } catch (NoSuchPartException e) {
            logger.log(Level.WARNING, "Failed to queue the request message.", e);
            return false;
        } catch (AlreadyClosedException e) {
            logger.log(Level.WARNING, "Failed to queue the request message.", e);
            deactivateDestination(destination);
            synchronized (failedProducers) {
                failedProducers.put(destination.getGoid(), System.currentTimeMillis());
            }
            return false;
        }
    }

    private boolean queueMessageWaitForResponse(Channel channel,
                                                String queueName,
                                                AMQPDestination destination,
                                                String routingKey,
                                                Message requestMsg,
                                                Message responseMsg,
                                                HashMap amqpProperties) {
        try {
            String correlationId = null;
            if (AMQPDestination.OutboundCorrelationBehaviour.GENERATE_CORRELATION_ID == destination.getOutboundCorrelationBehaviour()) {
                correlationId = clusterNodeId + "-" + nodeMessageId.getAndIncrement();
            }
            String messageId = clusterNodeId + "-" + nodeMessageId.getAndIncrement();

            ReplyConsumer replyConsumer = new ReplyConsumer(channel, correlationId == null ? messageId : correlationId);
            String consumerTag = channel.basicConsume(queueName, replyConsumer);

            AMQP.BasicProperties.Builder propertiesBuilder = createNewPropertiesBuilder(amqpProperties).
                    contentType(requestMsg.getMimeKnob().getOuterContentType().getFullValue()).
                    messageId(messageId).
                    replyTo(queueName).
                    timestamp(new Date());

            final HeadersKnob amqpHeadersKnob = requestMsg.getKnob(HeadersKnob.class);

            Map<String, Object> messageHeaders = new HashMap<String, Object>();

            if (amqpHeadersKnob != null) {
                Collection<Header> headers = amqpHeadersKnob.getHeaders();

                HashMap<String, Object> propertyMap = new HashMap<>();

                for (Header header : headers) {
                    propertyMap.put(header.getKey(), header.getValue());
                }

                for (String key : propertyMap.keySet()) {
                    messageHeaders.put(key, propertyMap.get(key));
                }
                propertiesBuilder.headers(messageHeaders);
            }

            if (AMQPDestination.OutboundCorrelationBehaviour.GENERATE_CORRELATION_ID == destination.getOutboundCorrelationBehaviour()) {
                propertiesBuilder = propertiesBuilder.correlationId(correlationId);
            }
            channel.basicPublish(destination.getExchangeName(),
                    routingKey,
                    propertiesBuilder.build(),
                    IOUtils.slurpStream(requestMsg.getMimeKnob().getEntireMessageBodyAsInputStream()));
            boolean failed = false;
            synchronized (replyConsumer) {
                if (!replyConsumer.isDone()) {
                    try {
                        replyConsumer.wait(responseTimeout);
                    } catch (InterruptedException e) {
                    }

                    if (!replyConsumer.isDone()) {
                        failed = true;
                    }

                    channel.basicCancel(consumerTag);
                    replyConsumer.stop();
                }
            }
            if (failed) {
                return false;
            } else {
                replyConsumer.initializeMessage(stashManagerFactory.createStashManager(), responseMsg, destination.getServiceGoid());
                return true;
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to queue the request message.", e);
            return false;
        } catch (NoSuchPartException e) {
            logger.log(Level.WARNING, "Failed to queue the request message.", e);
            return false;
        } catch (AlreadyClosedException e) {
            logger.log(Level.WARNING, "Failed to queue the request message.", e);
            deactivateDestination(destination);
            synchronized (failedProducers) {
                failedProducers.put(destination.getGoid(), System.currentTimeMillis());
            }
            return false;
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // see if the any of the cluster properties we care about have been changed

        if (event instanceof PolicyCacheEvent.Deleted) {
            // A service policy was deleted.
            logger.log(Level.INFO, "Service has been removed, refreshing AMQP Destinations...");
            updateAndValidateDestinations();
            return;
        }

        if (event instanceof ServiceEnablementEvent) {
            // The Service Cache has changed in some way, we need to re-evaluate
            // the AMQP Destinations to ensure they still point to active,
            // legitimate services.
            logger.log(Level.INFO, "Services have changed, refreshing AMQP Destinations...");
            updateAndValidateDestinations();
            return;
        }

        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eiEvent = (EntityInvalidationEvent) event;

            if (ClusterProperty.class.equals(eiEvent.getEntityClass())) {
                handleClusterPropertyChange(eiEvent);
                return;
            } else if (SsgActiveConnector.class.equals(eiEvent.getEntityClass())) {
                if (eiEvent.getSource() instanceof SsgActiveConnector) {
                    SsgActiveConnector ssgActiveConnector = (SsgActiveConnector) eiEvent.getSource();
                    if (ssgActiveConnector.getType().equals(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP)) {
                        logger.log(Level.INFO, "AMQP Settings have changed, refreshing AMQP Destinations...");
                        deactivateService(((EntityInvalidationEvent) event).getEntityIds());
                        updateAndValidateDestinations();
                    }
                }
            }
        }

    }

    private void handleClusterPropertyChange(EntityInvalidationEvent eiEvent) {
        for (Goid oid : eiEvent.getEntityIds()) {
            try {
                ClusterProperty cp = clusterPropertyManager.findByPrimaryKey(oid);
                if (cp != null) {
                    if (ModuleLoadListener.AMQP_RESPONSE_TIMEOUT_UI_PROPERTY.equals(cp.getName())) {
                        try {
                            responseTimeout = Long.parseLong(cp.getValue());
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, "Error loading cluster-wide property " + ModuleLoadListener.AMQP_RESPONSE_TIMEOUT_UI_PROPERTY + ". Using default value.", e);
                            responseTimeout = ModuleLoadListener.DEFAULT_AMQP_RESPONSE_TIMEOUT_MS;
                        }
                    } else if (ModuleLoadListener.AMQP_MESSAGE_MAX_BYTES_UI_PROPERTY.equals(cp.getName())) {
                        try {
                            maxMessageSize = Integer.parseInt(cp.getValue());
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, "Error loading cluster-wide property " + ModuleLoadListener.AMQP_MESSAGE_MAX_BYTES_UI_PROPERTY + ". Using default value.", e);
                            maxMessageSize = ModuleLoadListener.DEFAULT_AMQP_MESSAGE_MAX_BYTES;
                        }
                    } else if (ModuleLoadListener.AMQP_CONNECT_ERROR_SLEEP_UI_PROPERTY.equals(cp.getName())) {
                        try {
                            inboundConnectTimeout = Long.parseLong(cp.getValue());
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, "Error loading cluster-wide property " + ModuleLoadListener.AMQP_CONNECT_ERROR_SLEEP_UI_PROPERTY + ". Using default value.", e);
                            inboundConnectTimeout = ModuleLoadListener.DEFAULT_AMQP_CONNECT_ERROR_SLEEP_MS;
                        }
                    }
                } else {
                    // cluster-wide property was deleted. At this point, we do not know which cluster-wide property was deleted.
                    // So, re-load all cluster-wide properties.
                    loadClusterProperties();
                }
            } catch (FindException e) {
                logger.log(Level.FINE, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    private void updateAndValidateDestinations() {
        SsgActiveConnector[] newSsgActiveConnectors = new SsgActiveConnector[0];
        try {
            newSsgActiveConnectors = ssgActiveConnectorManager.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP).toArray(new SsgActiveConnector[0]);

            HashMap<Goid, AMQPDestination> oldDestinations = (HashMap<Goid, AMQPDestination>) destinations.clone();

            for (SsgActiveConnector currentSsgActiveConnector : newSsgActiveConnectors) {
                AMQPDestination currentDestination = AMQPDestinationHelper.ssgConnectorToAmqpDestination(currentSsgActiveConnector);
                // Check the new destination vs the existing destination,
                // See if the service it points to has changed.
                Goid serviceGOID = currentDestination.getServiceGoid();
                PublishedService service = serviceCache.getCachedService((serviceGOID == null ? new Goid(0, -1) : currentDestination.getServiceGoid()));

                if (currentDestination.isInbound() && service == null) {
                    // The service isn't available, and this destination will not be processed.
                    logger.log(Level.WARNING, "Inbound destination \"" + currentDestination.getName() + "\" is not available: The service policy it's configured to call during processing is missing.");
                } else if (currentDestination.isInbound() && service.isDisabled()) {
                    // The service is disabled, and this destination will also not be processed.
                    logger.log(Level.WARNING, "Inbound destination \"" + currentDestination.getName() + "\" is not available: The service policy it's configured to call during processing is disabled.");
                } else if (currentDestination.isInbound() && (!currentDestination.isEnabled())) {
                    // The user has requested the service stop draining the queue.
                    logger.log(Level.WARNING, "Inbound destination \"" + currentDestination.getName() + "\" is not enabled: \"Stop Listening on this Queue\" is on");
                } else {
                    // This destination passes the service test.
                    oldDestinations.remove(currentDestination.getGoid());

                    // Activate it.
                    if (currentDestination.isInbound()) {
                        activateConsumerDestination(currentDestination);
                    } else {
                        activateProducerDestination(currentDestination);
                    }
                }
            }

            /*Now iterate through the remaining destinations in oldDestinations, and clear your serverChannel,
            clientChannel, and destinations with that.*/
            for (Map.Entry<Goid, AMQPDestination> toRemoveDestinations : oldDestinations.entrySet()) {
                destinations.remove(toRemoveDestinations.getKey());
                if (toRemoveDestinations.getValue().isInbound()) {
                    //It was a server channel, Remove from server channel.
                    deactivateDestination(toRemoveDestinations.getValue());
                } else {
                    //It was a client channel, remove from client channels.
                    deactivateDestination(toRemoveDestinations.getValue());
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Something went wrong while fetching AMQP destinations", e);
        }
    }

    private void deactivateService(Goid[] oids) {
        for (Goid oid : oids) {
            AMQPDestination destination = destinations.get(oid);
            if (destination != null) {
                deactivateDestination(destination);
            }
        }
    }

    /**
     * The cached channel. The cache is considered to be expired when the reference count is
     * less than or equal to zero and either the connection has been idle for too long or
     * the maximum age for the channel has been passed.
     */
    protected static class CachedChannel {

        private final AtomicInteger referenceCount = new AtomicInteger(0);
        private final long createdTime = System.currentTimeMillis();
        private final AtomicLong lastAccessTime = new AtomicLong(createdTime);

        private final Channel clientChannel;

        CachedChannel(final Channel channel) {
            this.clientChannel = channel;
        }

        public Channel getClientChannel() {
            lastAccessTime.set(System.currentTimeMillis());
            return clientChannel;
        }

        public void incrementRef() {
            referenceCount.incrementAndGet();
        }

        public void decrementRef() {
            referenceCount.decrementAndGet();
        }

        public boolean isExpired() {
            long now = System.currentTimeMillis();
            final AMQPDestinationManagerConfig cacheConfig = cacheConfigReference.get();

            if (referenceCount.get() <= 0 && now > createdTime + cacheConfig.maximumAge && cacheConfig.maximumAge > 0) {
                logger.log(Level.WARNING, "AMQP Connection is evicted because it's past the maximum age allowed.");
                return true;
            } else if (referenceCount.get() <= 0 && now - lastAccessTime.get() > cacheConfig.maximumIdleTime && cacheConfig.maximumIdleTime > 0) {
                logger.log(Level.WARNING, "AMQP Connection is evicted because it's been idle for too long.");
                return true;
            }

            return false;
        }
    }

    /**
     * Bean for cache configuration.
     */
    private static final class AMQPDestinationManagerConfig {
        private final long maximumAge;
        private final long maximumIdleTime;

        private AMQPDestinationManagerConfig(final long maximumAge, final long maximumIdleTime) {
            this.maximumAge = maximumAge;
            this.maximumIdleTime = maximumIdleTime;
        }
    }

    /**
     * Validates the range that is provided; if the value is not valid then it will use the
     * default value that is provided.
     *
     * @param value        the value to validate
     * @param defaultValue the default value that will be used if not valid
     * @param min          the minimum value
     * @param max          the maximum value
     * @param description  the description
     * @param <T>
     * @return the value that is valid
     */
    private <T extends Number> T rangeValidate(final T value,
                                               final T defaultValue,
                                               final T min,
                                               final T max,
                                               final String description) {
        T validatedValue;

        if (value.longValue() < min.longValue()) {
            logger.log(Level.WARNING,
                    "Configuration value for {0} is invalid ({1} minimum is {2}), using default value ({3}).",
                    new Object[]{description, value, min, defaultValue});
            validatedValue = defaultValue;
        } else if (value.longValue() > max.longValue()) {
            logger.log(Level.WARNING,
                    "Configuration value for {0} is invalid ({1} maximum is {2}), using default value ({3}).",
                    new Object[]{description, value, max, defaultValue});
            validatedValue = defaultValue;
        } else {
            validatedValue = value;
        }

        return validatedValue;
    }

    private Channel activateConsumerDestinationHelper(AMQPDestination destination) throws FindException, ParseException, IOException {

        ConnectionFactory connectionFactory = createNewConnectionFactory();
        if (destination.getUsername() != null) {
            connectionFactory.setUsername(destination.getUsername());

            connectionFactory.setPassword(new String(securePasswordManager.decryptPassword(
                    securePasswordManager.findByPrimaryKey(destination.getPasswordGoid()).getEncodedPassword())));

        }
        if (destination.getVirtualHost() != null) {
            connectionFactory.setVirtualHost(destination.getVirtualHost());
        }

        initSSLSettings(destination, connectionFactory);

        ExecutorService es = Executors.newFixedThreadPool(destination.getThreadPoolSize());

        Address[] addresses = getAddresses(destination);

        Connection connection = connectionFactory.newConnection(es, addresses);
        Channel channel = connection.createChannel();
        return channel;


    }

    private Channel activateProducerDestinationHelper(AMQPDestination destination) throws IOException, FindException, ParseException {

        ConnectionFactory connectionFactory = createNewConnectionFactory();
        if (destination.getUsername() != null) {
            connectionFactory.setUsername(destination.getUsername());

            connectionFactory.setPassword(new String(securePasswordManager.decryptPassword(
                    securePasswordManager.findByPrimaryKey(destination.getPasswordGoid()).getEncodedPassword())));

        }
        if (destination.getVirtualHost() != null) {
            connectionFactory.setVirtualHost(destination.getVirtualHost());
        }

        initSSLSettings(destination, connectionFactory);

        Address[] addresses = getAddresses(destination);

        Connection connection = connectionFactory.newConnection(addresses);
        Channel channel = connection.createChannel();
        return channel;

    }

    protected ConnectionFactory createNewConnectionFactory() {
        ConnectionFactory cf = new ConnectionFactory();
        // Setting automatic recovery and topology recovery so that we can reconnect AMQP disconnects
        cf.setAutomaticRecoveryEnabled(true);
        cf.setTopologyRecoveryEnabled(true);
        return cf;
    }

    protected void initSSLSettings(AMQPDestination destination, ConnectionFactory factory) {
        if (!destination.isUseSsl()) {
            return;
        }
        try {
            KeyManager[] keyManagers = null;
            if (destination.getSslClientKeyId() != null) {
                SsgKeyEntry keyEntry = null;
                if ("-1:".equals(destination.getSslClientKeyId())) {
                    keyEntry = defaultKey.getSslInfo();
                } else {
                    String[] parts = destination.getSslClientKeyId().split(":");
                    keyEntry = keyStoreManager.lookupKeyByKeyAlias(parts.length == 1 ? null : parts[1], new Goid(parts[0]));
                }
                keyManagers = new KeyManager[]{new SingleCertX509KeyManager(
                        keyEntry.getCertificateChain(), keyEntry.getPrivate())};
            } else {
                keyManagers = new KeyManager[0];
            }

            Provider provider = JceProvider.getInstance().getProviderFor("SSLContext.TLSv1");
            SSLContext sslContext = SSLContext.getInstance("TLSv1", provider);
            sslContext.init(keyManagers, new TrustManager[]{trustManager}, secureRandom);

            factory.useSslProtocol(sslContext);
        } catch (IOException | FindException | KeyStoreException | NoSuchAlgorithmException |
                KeyManagementException | UnrecoverableKeyException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * @return an array of Address objects extracted from the destination
     */
    protected Address[] getAddresses(AMQPDestination destination) {
        Address[] addresses = new Address[destination.getAddresses().length];
        for (int i = 0; i < addresses.length; i++) {
            String[] address = destination.getAddresses()[i].split(":");
            addresses[i] = new Address(address[0], Integer.parseInt(address[1]));
        }
        return addresses;
    }

}