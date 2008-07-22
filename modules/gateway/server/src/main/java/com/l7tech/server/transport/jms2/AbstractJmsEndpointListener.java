package com.l7tech.server.transport.jms2;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.JMSEvent;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueReceiver;
import javax.naming.NamingException;
import java.beans.PropertyChangeEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for JmsEndpointListener implementations.  This class provides the common
 * thread life-cycle operations plus code that listens to a single jms endpoint.
 * <br/>
 * The system will always default to the multi-threaded mode for message processing.  However, the "legacy"
 * serial processing implementation can be called upon as a fallback.  This class contains the common
 * code used in both cases.
 * <br/>
 * All request messages are delegated to the handleMessage() method.  It is in this method that the
 * implementation for synchronously vs. asynchronously processing of messages will be handled.
 *
 * @author: vchan
 */
public abstract class AbstractJmsEndpointListener implements JmsEndpointListener {

    private static final Logger _logger = Logger.getLogger(AbstractJmsEndpointListener.class.getName());

    /*
     * Is there a better place for these properties?
     */
    protected static final String PROPERTY_ERROR_SLEEP = "ioJmsErrorSleep";
    protected static final String PROPERTY_MAX_SIZE = "ioJmsMessageMaxBytes";
    protected static final int MAXIMUM_OOPSES = 5;
    /** Set to five seconds so that the uninterruptible (!) poll doesn't pause server shutdown for too long. */
    protected static final long RECEIVE_TIMEOUT = 5 * 1000;
    protected static final long SHUTDOWN_TIMEOUT = 7 * 1000;
    protected static final int OOPS_RETRY = 5000; // Five seconds
    protected static final int DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    protected static final int MIN_OOPS_SLEEP = 10 * 1000; // 10 seconds
    protected static final int MAX_OOPS_SLEEP = TimeUnit.DAYS.getMultiplier(); // 24 hours
    protected static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 mins;
    protected static final int DEFAULT_MAX_SIZE = 5242880;

    /** The amount of time the thread sleeps when the MAXIMUM_OOPSES limit is reached */
    private final AtomicInteger oopsSleep = new AtomicInteger(DEFAULT_OOPS_SLEEP);

    /** The properties for the Jms endpoint that the listener is processing messages on */
    protected final JmsEndpointConfig _endpointCfg;

    /** The listener thread that performs the polling loop */
    protected final ListenerThread _listener;

    /** Flag specifying whether the listener has started */
    private boolean _started;

    // Runtime stuff
    private boolean _stop;
    private final Object sync = new Object();
    private Thread _thread;
    private long lastStopRequestedTime;
    private long lastAuditErrorTime;

    /**
     * The only constructor for the JMS Message Receivers
     *
     * @param endpointCfg
     */
    public AbstractJmsEndpointListener(final JmsEndpointConfig endpointCfg) {

        this._endpointCfg = endpointCfg;

        // create the ListenerThread
        this._listener = new ListenerThread();
    }

    /**
     * Returns a JmsBag object that contains a Connection/Session pair used for processing a request.
     * <ul>
     * <li>In single-threaded mode, a single session (JmsBag instance) can be used to handle all requests.</li>
     * <li>In multi-threaded mode, each messaging processing thread will need to have it's own session.
     * Created from a single connection</li>
     * </ul>
     *
     * @return
     * @throws JMSException
     * @throws NamingException
     * @throws JmsConfigException
     */
    protected abstract JmsBag getJmsBag() throws JMSException, NamingException, JmsConfigException;

    /**
     * Return a consumer that can read messages on the inbound Jms queue.
     *
     * @return JMS QueueReceiver for the inbound queue
     * @throws JMSException
     * @throws NamingException
     * @throws JmsConfigException
     */
    protected abstract QueueReceiver getConsumer() throws JMSException, NamingException, JmsConfigException;

    /**
     * Method used to ensure that the connection (javax.jms.Connection) used to communicate with the endpoint
     * has been started.
     *
     * @throws NamingException when a JmsBag could not be properly obtained
     * @throws JmsConfigException when a JmsBag could not be properly obtained
     * @throws JMSException when a JmsBag could not be properly obtained
     */
    protected void ensureConnectionStarted() throws NamingException, JmsConfigException, JMSException {
        synchronized(sync) {
            boolean ok = false;
            String message = null;
            try {
                JmsBag bag = getJmsBag();
                Connection conn = bag.getConnection();
                conn.start(); // should be ignored if started
                ok = true;
            } catch (JMSException e) {
                message = ExceptionUtils.getMessage(e);
                throw e;
            } catch (NamingException e) {
                message = ExceptionUtils.getMessage(e);
                throw e;
            } catch (JmsConfigException e) {
                message = ExceptionUtils.getMessage(e);
                throw e;
            } catch (RuntimeException e) {
                message = ExceptionUtils.getMessage(e);
                throw e;
            } finally {
                if (ok) {
                    if (!_started) {
                        _started = true;
                        fireConnected();
                    }
                } else {
                    fireConnectError(message);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("jms-listener-");
        s.append(_endpointCfg.getDisplayName());
        return s.toString();
    }

    /**
     * Starts the listener thread.
     *
     * @throws LifecycleException when an error is encountered in the thread startup
     */
    public void start() throws LifecycleException {
        synchronized(sync) {
            log(Level.FINE, JmsMessages.INFO_LISTENER_START, toString());
            _thread.start();
            log(Level.FINE, JmsMessages.INFO_LISTENER_STARTED, toString());
        }
    }

    /**
     * Tells the listener thread to stop.
     */
    public void stop() {
        synchronized(sync) {
            log(Level.FINE, JmsMessages.INFO_LISTENER_STOP, toString());
            _stop = true;
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
            if ( waitTime > 10 ) {
                _thread.join( SHUTDOWN_TIMEOUT );
            }
        } catch ( InterruptedException ie ) {
            Thread.currentThread().interrupt();
        }

        if ( _thread.isAlive() ) {
            log(Level.WARNING, JmsMessages.WARN_LISTENER_THREAD_ALIVE, this);
        }
    }

    /**
     * Perform cleanup of resources and reset the listener status.
     */
    protected void cleanup() {
        /*
         * Moved responsibility for cleanup to implementation class
         */
        _started = false;
    }

    /**
     * Returns flag specifying whether the listener is stopped.
     *
     * @return boolean flag
     */
    protected boolean isStop() {
        synchronized(sync) {
            return _stop;
        }
    }

    /**
     * Listens on the inbound jms queue for a request message.  If a message is present,
     * it is returned immediately to the caller.  Otherwise, the method will block on the
     * QueueReceiver.receive() call until the timeout period elapses and returns null.
     *
     * @return the request message if one exists on the queue, or null upon timeout
     * @throws JMSException
     * @throws NamingException
     * @throws JmsConfigException
     */
    private Message receiveMessage() throws JMSException, NamingException, JmsConfigException
    {
        QueueReceiver receiver = getConsumer();
        ensureConnectionStarted();

        Message jmsMessage = receiver.receive( RECEIVE_TIMEOUT );
        return jmsMessage;
    }

    /**
     * Perform the processing on a Jms message.  This is the point where an implementation
     * of the JmsEndpointListener would work in a synchronous / asynchronous manner.
     *
     * @param dequeueMessage the message to process
     * @throws JmsRuntimeException error encountered while processing the jms message
     */
    protected abstract void handleMessage(Message dequeueMessage) throws JmsRuntimeException;


    public JmsEndpointConfig getEndpointConfig() {
        return this._endpointCfg;
    }

    public long getJmsConnectionOid() {
        return this._endpointCfg.getConnection().getOid();
    }

    public long getJmsEndpointOid() {
        return this._endpointCfg.getEndpoint().getOid();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {

        if (PROPERTY_ERROR_SLEEP.equals(evt.getPropertyName())) {

            String stringValue = (String) evt.getNewValue();
            setErrorSleepTime(stringValue);

        } else if (PROPERTY_MAX_SIZE.equals(evt.getPropertyName())) {

            String stringValue = (String) evt.getNewValue();
            int newMaxSize = DEFAULT_MAX_SIZE;

            try {
                newMaxSize = Integer.parseInt( stringValue );
            } catch (NumberFormatException nfe) {
                _logger.log(Level.WARNING, "Ignoring invalid JMS message max size ''{0}'' (using default).", stringValue);
            }

            if ( newMaxSize < 0 ) {
                _logger.log(Level.WARNING, "Ignoring invalid JMS message max size ''{0}'' (using 0).", stringValue);
                newMaxSize = 0;
            }

            _logger.log(Level.CONFIG, "Updated JMS message max size to {0}.", newMaxSize);
            _endpointCfg.setMessageMaxSize(newMaxSize);
        }

        /*
         * What about the thread pool cluster properties?
         */
    }

    protected void fireConnected() {
        lastAuditErrorTime = 0L;
        fireEvent(new JMSEvent(this, Level.INFO, null,
                formatMessage(JmsMessages.INFO_EVENT_CONNECT_SUCCESS, _endpointCfg.getDisplayName())));
    }

    protected void fireConnectError(String message) {
        fireEvent(new JMSEvent(this, Level.WARNING,  null, formatMessage(
                        JmsMessages.INFO_EVENT_CONNECT_FAIL,
                        new Object[] {_endpointCfg.getDisplayName(), message}))
        );
    }

    /**
     * Fires a Jms event.
     *
     * @param event the jms event to fire
     */
    protected void fireEvent(JMSEvent event) {

        if (_endpointCfg.getApplicationContext() != null) {

            long timeNow = System.currentTimeMillis();

            if ((lastAuditErrorTime+OOPS_AUDIT) < timeNow) {
                lastAuditErrorTime = timeNow;
                _endpointCfg.getApplicationContext().publishEvent(event);

            } else {
                log(Level.INFO, JmsMessages.INFO_EVENT_NOT_PUBLISHED, new Object[0]);
            }

        } else {
            log(Level.WARNING, JmsMessages.INFO_EVENT_NOT_PUBLISHABLE, event.getMessage());
        }
    }


    protected void setErrorSleepTime(String stringValue) {
        long newErrorSleepTime = DEFAULT_OOPS_SLEEP;

        try {
            newErrorSleepTime = TimeUnit.parse(stringValue, TimeUnit.SECONDS);
        } catch (NumberFormatException nfe) {
            _logger.log(Level.WARNING, "Ignoring invalid JMS error sleep time ''{0}'' (using default).", stringValue);
        }

        if ( newErrorSleepTime < MIN_OOPS_SLEEP ) {
            _logger.log(Level.WARNING, "Ignoring invalid JMS error sleep time ''{0}'' (using minimum).", stringValue);
            newErrorSleepTime = MIN_OOPS_SLEEP;
        } else if ( newErrorSleepTime > MAX_OOPS_SLEEP ) {
            _logger.log(Level.WARNING, "Ignoring invalid JMS error sleep time ''{0}'' (using maximum).", stringValue);
            newErrorSleepTime = MAX_OOPS_SLEEP;
        }

        _logger.log(Level.CONFIG, "Updated JMS error sleep time to {0}ms.", newErrorSleepTime);
        oopsSleep.set((int)newErrorSleepTime);
    }


    /**
     * Listener thread responsible for receiving messages from the Jms endpoint.
     */
    private class ListenerThread implements Runnable {

        /**
         * Default constructor.
         */
        private ListenerThread() {
            _thread = new Thread(this, toString());
            _thread.setDaemon(true);
        }

        /**
         *
         */
        public final void run() {

            int oopses = 0;
            log(Level.INFO, JmsMessages.INFO_LISTENER_POLLING_START, _endpointCfg.getEndpoint().getDestinationName());

            try {
                Message jmsMessage;
                while ( !isStop() ) {
                    try {
//                        _logger.finest( "Polling for a message on " + _inboundRequestEndpoint.getDestinationName() );
                        jmsMessage = receiveMessage();

                        if ( jmsMessage != null && !isStop() ) {

                            log(Level.FINE, JmsMessages.INFO_LISTENER_RECEIVE_MSG, _endpointCfg.getEndpoint().getDestinationName());
                            oopses = 0;

                            // process on the message
                            handleMessage(jmsMessage);

                        }
                    } catch ( Throwable e ) {
                        if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
                            continue;
                        }

                        log(Level.WARNING, formatMessage(
                                JmsMessages.WARN_LISTENER_RECEIVE_ERROR,
                                _endpointCfg.getEndpoint().getDestinationName()), e);

                        cleanup();

                        if ( ++oopses < MAXIMUM_OOPSES ) {
                            // sleep for a short period of time before retrying
                            try {
                                Thread.sleep(OOPS_RETRY);
                            } catch ( InterruptedException e1 ) {
                                log(Level.INFO, JmsMessages.INFO_LISTENER_POLLING_INTERRUPTED, new Object[] {"retry interval"});
                            }

                        } else {
                            // max oops reached .. sleep for a longer period of time before retrying
                            int sleepTime = oopsSleep.get();

                            log(Level.WARNING, JmsMessages.WARN_LISTENER_MAX_OOPS_REACHED, new Object[] {_endpointCfg.getEndpoint(), MAXIMUM_OOPSES, sleepTime });

                            try {
                                Thread.sleep(sleepTime);
                            } catch ( InterruptedException e1 ) {
                                log(Level.INFO, JmsMessages.INFO_LISTENER_POLLING_INTERRUPTED, new Object[] {"sleep interval"});
                            }
                        }
                    }
                }
            } finally {

                log(Level.INFO, JmsMessages.INFO_LISTENER_POLLING_STOP, _endpointCfg.getEndpoint().getDestinationName());
                cleanup();
            }
        }
    }

    protected void log(Level level, String jmsMessageKey, Object parm) {

        if (parm == null)
            log(level, jmsMessageKey, new Object[0]);
        else
            log(level, jmsMessageKey, new Object[] {parm});
    }

    protected void log(Level level, String jmsMessageKey, Object[] parm) {

        _logger.log(level, jmsMessageKey, parm);
    }

    protected void log(Level level, String jmsMessageKey, Throwable ex) {

        _logger.log(level, jmsMessageKey, ex);
    }

    protected String formatMessage(String jmsMessageKey, Object[] parm) {

        return java.text.MessageFormat.format(jmsMessageKey, parm);
    }

    protected String formatMessage(String jmsMessageKey, Object parm) {

        return formatMessage(jmsMessageKey, new Object[] {parm});
    }
}

