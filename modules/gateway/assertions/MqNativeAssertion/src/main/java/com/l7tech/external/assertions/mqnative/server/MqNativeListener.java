package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
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

    // Set to five seconds so that the un-interrupt-able poll doesn't pause server shutdown for too long.
    static final int RECEIVE_TIMEOUT = 5 * 1000;

    static final long SHUTDOWN_TIMEOUT = 7 * 1000;
    static final int OOPS_RETRY = 5000; // Five seconds
    static final int DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    static final int MIN_OOPS_SLEEP = 10 * 1000; // 10 seconds
    static final int MAX_OOPS_SLEEP = TimeUnit.DAYS.getMultiplier(); // 24 hours
    static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 minutes

    /** The properties for the MQ native resource that the listener is processing messages on */
    final SsgActiveConnector ssgActiveConnector;
    final ApplicationEventPublisher eventPublisher;
    private final SecurePasswordManager securePasswordManager;

    final MqNativeListenerThread listenerThread;
    final MqNativeClient mqNativeClient;

    // Runtime stuff
    private boolean threadStopped;
    private final Object sync = new Object();
    private long lastStopRequestedTime;
    private long lastAuditErrorTime;

    public MqNativeListener(@NotNull final SsgActiveConnector ssgActiveConnector,
                            @NotNull final ApplicationEventPublisher eventPublisher,
                            @NotNull final SecurePasswordManager securePasswordManager) throws MqNativeConfigException {
        this.ssgActiveConnector = ssgActiveConnector;
        this.eventPublisher = eventPublisher;
        this.securePasswordManager = securePasswordManager;
        this.mqNativeClient = buildMqNativeClient();
        this.listenerThread = new MqNativeListenerThread(this, toString());
    }

    public String getDisplayName() {
        final StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder.append( ssgActiveConnector.getName() );
        stringBuilder.append( " (#" );
        stringBuilder.append( ssgActiveConnector.getOid() );
        stringBuilder.append( ",v" );
        stringBuilder.append( ssgActiveConnector.getVersion() );
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
            log(Level.WARNING, MqNativeMessages.WARN_LISTENER_THREAD_ALIVE, this);
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

    SsgActiveConnector getSsgActiveConnector() {
        return this.ssgActiveConnector;
    }

    /**
     *
     * @throws MqNativeConfigException
     * @return
     */
    protected MqNativeClient buildMqNativeClient() throws MqNativeConfigException {
        MQQueueManager queueManager;
        MQQueue targetQueue;
        MQQueue specifiedReplyQueue = null;

        try {
            queueManager = new MQQueueManager(getConnectorProperty( PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME ), buildQueueManagerConnectProperties());

            final int openOps = MQC.MQOO_INPUT_AS_Q_DEF | MQC.MQOO_BROWSE | MQC.MQOO_INQUIRE; // TODO make these configurable
            targetQueue = queueManager.accessQueue(getConnectorProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME), openOps);

            MqNativeReplyType replyType = MqNativeReplyType.valueOf( getConnectorProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE) );
            String specifiedReplyQueueName = getConnectorProperty( PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME );
            if (MqNativeReplyType.REPLY_SPECIFIED_QUEUE == replyType && !StringUtils.isEmpty(specifiedReplyQueueName) ) {
                 specifiedReplyQueue = queueManager.accessQueue(specifiedReplyQueueName, MQC.MQOO_OUTPUT);
            }
        } catch (Exception e) {
            throw new MqNativeConfigException("Error while attempting to access QueueManager and Queue.", e);
        }

        if (targetQueue == null) {
            throw new MqNativeConfigException("Failed to instantiate MQ Queue: null");
        }

        return new MqNativeClient(queueManager, targetQueue, specifiedReplyQueue, RECEIVE_TIMEOUT, new MqNativeClient.MqNativeConnectionListener() {
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
        return MqNativeUtils.buildQueueManagerConnectProperties( ssgActiveConnector, securePasswordManager );
    }

    protected String getConnectorProperty( final String name ) {
        return ssgActiveConnector.getProperty( name );
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
            if (readEx.reasonCode != 2033) { // queue is empty
                throw readEx;
            }
        }
        return readMsg;
    }

    /**
     * Execute work in a callback function that requires a MQ native client.
     * @param callback the work logic requiring a SFTP client
     * @return the any result(s) from the work done
     * @throws com.ibm.mq.MQException if there's an error
     */
    <R> R doWithMqNativeClient( final Functions.BinaryThrows<R,MQQueue,MQGetMessageOptions,MQException> callback ) throws MQException {
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
                MqNativeMessages.INFO_EVENT_CONNECT_FAIL,
                getDisplayName(), message )));
    }

    private void fireEvent(TransportEvent event) {
        if ( eventPublisher != null) {
            long timeNow = System.currentTimeMillis();
            if ((lastAuditErrorTime+ (long) OOPS_AUDIT) < timeNow) {
                lastAuditErrorTime = timeNow;
                eventPublisher.publishEvent( event );
            } else {
                log(Level.INFO, MqNativeMessages.INFO_EVENT_NOT_PUBLISHED);
            }
        } else {
            log(Level.WARNING, MqNativeMessages.INFO_EVENT_NOT_PUBLISHABLE, event.getMessage());
        }
    }

    void log(Level level, String messageKey, Object... parm) {
        logger.log(level, messageKey, parm);
    }

    void log(Level level, String messageKey, Throwable ex) {
        logger.log(level, messageKey, ex);
    }

    protected void setErrorSleepTime(String stringValue) {
        long newErrorSleepTime = DEFAULT_OOPS_SLEEP;

        try {
            newErrorSleepTime = TimeUnit.parse(stringValue, TimeUnit.SECONDS);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Ignoring invalid MQ error sleep time ''{0}'' (using default).", stringValue);
        }

        if ( newErrorSleepTime < MIN_OOPS_SLEEP ) {
            logger.log(Level.WARNING, "Ignoring invalid MQ error sleep time ''{0}'' (using minimum).", stringValue);
            newErrorSleepTime = MIN_OOPS_SLEEP;
        } else if ( newErrorSleepTime > MAX_OOPS_SLEEP ) {
            logger.log(Level.WARNING, "Ignoring invalid MQ error sleep time ''{0}'' (using maximum).", stringValue);
            newErrorSleepTime = MAX_OOPS_SLEEP;
        }

        logger.log(Level.CONFIG, "Updated MQ error sleep time to {0}ms.", newErrorSleepTime);
        // oopsSleep.set((int)newErrorSleepTime);
    }
}