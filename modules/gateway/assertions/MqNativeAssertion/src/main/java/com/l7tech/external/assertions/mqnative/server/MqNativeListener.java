package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.external.assertions.mqnative.server.MqNativeClient.ClientBag;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Functions.NullaryThrows;
import com.l7tech.util.Functions.UnaryThrows;
import com.l7tech.util.Option;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ibm.mq.constants.CMQC.MQRC_NO_MSG_AVAILABLE;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.external.assertions.mqnative.server.MqNativeMessages.INFO_EVENT_CONNECT_FAIL;
import static com.l7tech.external.assertions.mqnative.server.MqNativeMessages.INFO_EVENT_NOT_PUBLISHABLE;
import static com.l7tech.gateway.common.audit.SystemMessages.CONNECTOR_ERROR;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.util.Option.some;
import static java.text.MessageFormat.format;

/**
 * Abstract base class for MqNativeListener implementations.  This class provides the common
 * thread life-cycle operations plus code that listens to a single MQ resource.
 * <br/>
 * All request messages are delegated to the handleMessage() method.
 */
public abstract class MqNativeListener {
    private static final Logger logger = Logger.getLogger(MqNativeListener.class.getName());

    protected static final int MAXIMUM_OOPSES = 5;

    static final long SHUTDOWN_TIMEOUT = 7 * 1000;
    static final int DEFAULT_OOPS_RETRY = 5000; // Five seconds
    static final long DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    static final int MIN_OOPS_SLEEP = 10 * 1000; // 10 seconds
    static final int MAX_OOPS_SLEEP = TimeUnit.DAYS.getMultiplier(); // 24 hours
    static final int DEFAULT_OOPS_AUDIT = 0; // 0 seconds
    static final int DEFAULT_POLL_INTERVAL = 5 * 1000; // Set to five seconds so that the un-interrupt-able poll doesn't pause server shutdown for too long.

    private int oopsRetry = DEFAULT_OOPS_RETRY;

    /** The properties for the MQ native resource that the listener is processing messages on */
    final SsgActiveConnector ssgActiveConnector;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurePasswordManager securePasswordManager;

    final MqNativeListenerThread listenerThread;
    final MqNativeClient mqNativeClient;

    // Runtime stuff
    private boolean threadStopped;
    private final Object sync = new Object();
    private long lastStopRequestedTime;
    private long lastAuditErrorTime;
    private int concurrentId;
	private long preventAuditFloodPeriod;
    private Collection<AuditDetail> auditDetails = new LinkedList<AuditDetail>();

    public MqNativeListener(@NotNull final SsgActiveConnector ssgActiveConnector,
                                     final int concurrentId,
                            @NotNull final ApplicationEventPublisher eventPublisher,
                            @NotNull final SecurePasswordManager securePasswordManager,
                            @NotNull final ServerConfig serverConfig) throws MqNativeConfigException {
        this.ssgActiveConnector = ssgActiveConnector;
        this.concurrentId = concurrentId;
        this.eventPublisher = eventPublisher;
        this.securePasswordManager = securePasswordManager;
        this.mqNativeClient = buildMqNativeClient();
        this.listenerThread = new MqNativeListenerThread(this, toString());
        configureProperties(serverConfig);
    }

    public String getDisplayName() {
        final StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder.append( ssgActiveConnector.getName() );
        stringBuilder.append( " (#" );
        stringBuilder.append( ssgActiveConnector.getGoid() );
        stringBuilder.append( ",v" );
        stringBuilder.append( ssgActiveConnector.getVersion() );
        if ( concurrentId > 0 ) {
            stringBuilder.append( ",c" );
            stringBuilder.append( concurrentId );
        }
        stringBuilder.append( ")" );
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "MqNativeListener; " + getDisplayName();
    }

    /**
     * Perform the processing on a MQ message.  This is the point where an implementation
     * of the JmsEndpointListener would work in a synchronous / asynchronous manner.
     *
     * @param queueMessage the message to process
     * @throws MqNativeException error encountered while processing the MQ inbound message
     */
    abstract void handleMessage(MQMessage queueMessage) throws MqNativeException;

    abstract void auditError(final String message, @Nullable final Throwable exception);

    void auditError(final String message) {
        auditError(message, null);
    }

    void addAuditDetail(final String message, @Nullable final Throwable exception) {
        auditDetails.add(new AuditDetail(CONNECTOR_ERROR, new String[]{ssgActiveConnector.getType(), message}, exception));
    }

    void addAuditDetail(final String message) {
        addAuditDetail(message, null);
    }

    /**
     * Starts the listener thread.
     *
     * @throws com.l7tech.server.LifecycleException when an error is encountered in the thread startup
     */
    public void start() throws LifecycleException {
        synchronized(sync) {
            log(Level.FINE, MqNativeMessages.INFO_LISTENER_START, toString());
            listenerThread.start();
            log(Level.FINE, MqNativeMessages.INFO_LISTENER_STARTED, toString());
        }
    }

    /**
     * Tells the listener thread to stop.
     */
    public void stop() {
        synchronized(sync) {
            log(Level.FINE, MqNativeMessages.INFO_LISTENER_STOP, toString());
            threadStopped = true;
            lastStopRequestedTime = System.currentTimeMillis();
        }
    }

    /**
     * Give the listener thread a set amount of time to shutdown, before it gets interrupted.
     */
    public void ensureStopped() {
        long stopRequestedTime;
        synchronized(sync) {
            stop();
            stopRequestedTime = lastStopRequestedTime;
        }

        try {
            long waitTime = SHUTDOWN_TIMEOUT - (System.currentTimeMillis() - stopRequestedTime);
            if ( waitTime > 10L ) {
                listenerThread.join( SHUTDOWN_TIMEOUT );
            }
        } catch ( InterruptedException ie ) {
            Thread.currentThread().interrupt();
        }

        if ( listenerThread.isAlive() ) {
            auditError(format(MqNativeMessages.WARN_LISTENER_THREAD_ALIVE, this));
        }
    }

    /**
     * Returns flag specifying whether the listener is stopped.
     *
     * @return boolean flag
     */
    boolean isStop() {
        synchronized(sync) {
            return threadStopped;
        }
    }

    /**
     * Perform cleanup of resources and reset the listener status.
     */
    protected void cleanup() {
        ResourceUtils.closeQuietly(mqNativeClient);
    }

    /**
     * Build a client to make calls to the MQ server.
     * @throws MqNativeConfigException if configuration settings don't work
     * @return MqNativeClient
     */
    protected MqNativeClient buildMqNativeClient() throws MqNativeConfigException {
        final String queueManagerName = getConnectorProperty( PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME );
        final NullaryThrows<Hashtable,MqNativeConfigException> queueManagerProperties =
                new NullaryThrows<Hashtable,MqNativeConfigException>(){
            @Override
            public Hashtable call() throws MqNativeConfigException {
                return buildQueueManagerConnectProperties();
            }
        };
        final String queueName = getConnectorProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME);
        final MqNativeReplyType replyType = MqNativeReplyType.valueOf( getConnectorProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE) );
        final String specifiedReplyQueueName = getConnectorProperty( PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME );
        final Option<String> replyQueueName = MqNativeReplyType.REPLY_SPECIFIED_QUEUE == replyType &&
                !StringUtils.isEmpty(specifiedReplyQueueName) ?
                    some(specifiedReplyQueueName) :
                    Option.<String>none();

        return new MqNativeClient(
                queueManagerName,
                queueManagerProperties,
                queueName,
                replyQueueName,
                new MqNativeClient.MqNativeConnectionListener() {
                    @Override
                    public void notifyConnected() {
                        fireConnected();
                    }

                    @Override
                    public void notifyConnectionError(String message) {
                        fireConnectError( message );
                    }
        });
    }

    protected Hashtable buildQueueManagerConnectProperties() throws MqNativeConfigException {
        return MqNativeUtils.buildQueueManagerConnectProperties(ssgActiveConnector, securePasswordManager);
    }

    protected String getConnectorProperty( final String name ) {
        return ssgActiveConnector.getProperty(name);
    }

    /**
     * Listens on the inbound MQ queue for a request message.  If a message is present,
     * it is returned immediately to the caller.  Otherwise, it will wait until the
     * RECEIVE_TIMEOUT period elapses and returns null.
     *
     * @param queue the queue to read
     * @param mqGetMessageOptions options for getting the message
     * @return the request message if one exists on the queue, or null upon timeout
     * @throws com.ibm.mq.MQException when connecting to the MQ endpoint fails
     */
    MQMessage receiveMessage(MQQueue queue, MQGetMessageOptions mqGetMessageOptions) throws MQException
    {
        MQMessage readMsg = null;
        try {
            MQMessage tempReadMsg = new MQMessage();
            queue.get(tempReadMsg, mqGetMessageOptions);
            readMsg = tempReadMsg;
        } catch (MQException readEx) {
            if (readEx.getReason() != MQRC_NO_MSG_AVAILABLE) { // queue is empty
                throw readEx;
            }
        }
        return readMsg;
    }

    /**
     * Execute work in a callback function that requires a MQ native client.
     * @param callback the work logic requiring a MQ client
     * @return the any result(s) from the work done
     * @throws com.ibm.mq.MQException if there's an error
     * @throws MqNativeConfigException if there's a config error
     */
    <R> R doWithMqNativeClient( final UnaryThrows<R,ClientBag,MQException> callback ) throws MQException, MqNativeConfigException {
        return mqNativeClient.doWork( callback );
    }

    /**
     * Event class for use by MQ native listener.
     */
    public static class MqNativeEvent extends TransportEvent {
        private static final String NAME = "Connect";

        public MqNativeEvent(
                Object source,
                Level level,
                String ip,
                String message) {
            super(source, Component.GW_MQ_NATIVE_RECV, ip, level, NAME, message);
        }
    }

    private void fireConnected() {
        lastAuditErrorTime = 0L;
        fireEvent(new MqNativeEvent(this, Level.INFO, null,
                format(MqNativeMessages.INFO_EVENT_CONNECT_SUCCESS, getDisplayName())));
    }

    private void fireConnectError(String message) {
        fireEvent(new MqNativeEvent(this, Level.WARNING,  null, format(
                INFO_EVENT_CONNECT_FAIL,
                getDisplayName(), message )));
    }

    private void fireEvent(TransportEvent event) {
        if ( eventPublisher != null) {
            long timeNow = System.currentTimeMillis();
            if ((lastAuditErrorTime + preventAuditFloodPeriod) < timeNow) {
                lastAuditErrorTime = timeNow;
                event.setAuditDetails(auditDetails);
                eventPublisher.publishEvent(event);
                auditDetails.clear();
            } else {
                log(Level.INFO, MqNativeMessages.INFO_EVENT_NOT_PUBLISHED, event.getMessage());
            }
        } else {
            auditError(format(INFO_EVENT_NOT_PUBLISHABLE, event.getMessage()));
        }
    }

    void log(Level level, String messageKey, Object... parm) {
        logger.log(level, messageKey, parm);
    }

    void log(Level level, String messageKey, Throwable ex) {
        logger.log(level, messageKey, ex);
    }

    private void configureProperties(ServerConfig serverConfig) {
        listenerThread.setOopsSleep(getErrorSleepTime(serverConfig));
        listenerThread.setPollInterval(getPollInterval(serverConfig));

        preventAuditFloodPeriod = serverConfig.getTimeUnitProperty(MQ_PREVENT_AUDIT_FLOOD_PERIOD_PROPERTY, DEFAULT_OOPS_AUDIT);
    }

    private long getErrorSleepTime(ServerConfig serverConfig) {
        long newErrorSleepTime = serverConfig.getTimeUnitProperty(MQ_CONNECT_ERROR_SLEEP_PROPERTY, DEFAULT_OOPS_SLEEP);

        if ( newErrorSleepTime < MIN_OOPS_SLEEP ) {
            auditError(format("Ignoring invalid MQ error sleep time ''{0}'' (using minimum).", MQ_CONNECT_ERROR_SLEEP_PROPERTY));
            newErrorSleepTime = MIN_OOPS_SLEEP;
        } else if ( newErrorSleepTime > MAX_OOPS_SLEEP ) {
            auditError(format("Ignoring invalid MQ error sleep time ''{0}'' (using maximum).", MQ_CONNECT_ERROR_SLEEP_PROPERTY));
            newErrorSleepTime = MAX_OOPS_SLEEP;
        }

        logger.log(Level.CONFIG, "Updated MQ error sleep time to {0}ms.", newErrorSleepTime);
        return newErrorSleepTime;
    }

    private int getPollInterval(ServerConfig serverConfig) {
        int pollInterval = DEFAULT_POLL_INTERVAL;
        long pollIntervalLong = serverConfig.getTimeUnitProperty(MQ_LISTENER_POLLING_INTERVAL_PROPERTY, DEFAULT_POLL_INTERVAL);

        if (pollIntervalLong < Integer.MIN_VALUE || pollIntervalLong > Integer.MAX_VALUE) {
            auditError(format("Ignoring invalid MQ poll interval ''{0}'', cannot be cast to int without changing its value (using default).", DEFAULT_POLL_INTERVAL));
        } else {
            pollInterval = (int) pollIntervalLong;
        }

        logger.log(Level.CONFIG, "Updated MQ poll interval to {0}ms.", pollInterval);
        return pollInterval;
    }

    public void setOopsRetry(int retry) {
        oopsRetry = retry;
    }

    public int getOopsRetry() {
        return oopsRetry;
    }
}