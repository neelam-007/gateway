package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.asynch.JmsTask;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.JMSEvent;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
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

    private final Logger _logger;
    protected static final int DEFAULT_RECEIVER = 1;

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
    protected static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 minutes;

    /** The amount of time the thread sleeps when the MAXIMUM_OOPSES limit is reached */
    private final AtomicInteger oopsSleep = new AtomicInteger(DEFAULT_OOPS_SLEEP);

    /** The properties for the Jms endpoint that the listener is processing messages on */
    protected final JmsEndpointConfig _endpointCfg;

    /** The listener thread that performs the polling loop */
    protected final ListenerThread _listener;

    /** Flag specifying whether the listener has started */
    private boolean _started;

    // Runtime stuff
    protected final Object sync = new Object();
    protected JmsBag _jmsBag;
    protected List<JmsBag> receivers = new ArrayList<JmsBag>();
    private Destination _destination;
    private Queue _failureQueue;
    private boolean _stop;
    private Thread _thread;
    private long lastStopRequestedTime;
    private long lastAuditErrorTime;
    protected final JmsResourceManager resourceManager;

    /**
     * The only constructor for the JMS Message Receivers
     *
     * @param endpointCfg
     */
    public AbstractJmsEndpointListener(final JmsEndpointConfig endpointCfg, final Logger logger) {

        this._endpointCfg = endpointCfg;
        this._logger = logger;

        // create the ListenerThread
        this._listener = new ListenerThread();
        resourceManager = _endpointCfg.getApplicationContext().getBean("jmsResourceManager", JmsResourceManager.class);

    }

    /**
     * Returns a JmsBag object that contains a Connection/Session pair used for processing a request.
     * <ul>
     * <li>In single-threaded mode, a single session (JmsBag instance) can be used to handle all requests.</li>
     * <li>In multi-threaded mode, each messaging processing thread will need to have it's own session.
     * Created from a single connection</li>
     * </ul>
     */
    protected JmsBag getJmsBag() throws JMSException, NamingException, JmsConfigException, JmsRuntimeException {
        JmsBag bag;

        synchronized(sync) {
            bag = _jmsBag;
            if ( bag == null ) {
                _logger.finest( "Getting new JmsBag" );
                bag = resourceManager.borrowJmsBag(_endpointCfg);
                _jmsBag = bag;
            }
        }

        return bag;
    }

    protected Destination getDestination() throws NamingException, JmsConfigException, JMSException, JmsRuntimeException {
        synchronized(sync) {
            if ( _destination == null ) {
                _logger.finest( "Getting new destination" );
                final JmsBag bag = getJmsBag();
                final Context context = bag.getJndiContext();
                final String destinationName = _endpointCfg.getEndpoint().getDestinationName();
                _destination = JmsUtil.cast( context.lookup( destinationName ), Destination.class );
            }
            return _destination;
        }
    }

    protected Queue getFailureQueue() throws NamingException, JmsConfigException, JMSException, JmsRuntimeException {
        synchronized(sync) {
            if ( _failureQueue == null &&
                    _endpointCfg.isTransactional() &&
                    _endpointCfg.getEndpoint().getFailureDestinationName() != null)
            {
                _logger.finest( "Getting new FailureQueue" );
                final JmsBag bag = getJmsBag();
                final Context context = bag.getJndiContext();
                final String failureDestinationName = _endpointCfg.getEndpoint().getFailureDestinationName();
                _failureQueue = JmsUtil.cast( context.lookup( failureDestinationName ), Queue.class );
            }
            return _failureQueue;
        }
    }

    /**
     * Method used to ensure that the connection (javax.jms.Connection) used to communicate with the endpoint
     * has been started.
     *
     * @throws NamingException when a JmsBag could not be properly obtained
     * @throws JmsConfigException when a JmsBag could not be properly obtained
     * @throws JMSException when a JmsBag could not be properly obtained
     */
    protected void ensureConnectionStarted() throws NamingException, JmsConfigException, JMSException, JmsRuntimeException {
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
            } catch (JmsRuntimeException e) {
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
        StringBuilder s = new StringBuilder("jms-listener-");
        s.append(identify());
        return s.toString();
    }

    private String identify() {
        StringBuilder s = new StringBuilder(_endpointCfg.getDisplayName());
        s.append("[");
        s.append(_endpointCfg.getEndpoint().getGoid());
        s.append("v");
        s.append(_endpointCfg.getEndpoint().getVersion());
        s.append(",");
        s.append(_endpointCfg.getConnection().getGoid());
        s.append("v");
        s.append(_endpointCfg.getConnection().getVersion());
        s.append("]");
        return s.toString();
    }

    /**
     * Starts the listener thread.
     *
     * @throws LifecycleException when an error is encountered in the thread startup
     */
    @Override
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
    @Override
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
    @Override
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
        // close the consumer

        _destination = null;

        _failureQueue = null;

        // close the Jms connection artifacts
        if ( _jmsBag != null ) {
            // this will close the session and cause rollback if transacted
            try {
                // return the jms bag
                resourceManager.returnJmsBag(_jmsBag);
                for (JmsBag jmsBag: receivers) {
                    resourceManager.returnJmsBag(jmsBag);
                }
            } catch (JmsRuntimeException e) {
                handleCleanupError("Return Jms Session", e);
            }
            _jmsBag = null;
        }
        resourceManager.invalidate(_endpointCfg);

        _started = false;
    }

    private void handleCleanupError( final String detail, final Exception exception ) {
        _logger.log(
                Level.WARNING,
                "Error during AbstractJmsEndpointListener cleanup ("+detail+"): " + ExceptionUtils.getMessage( exception ),
                ExceptionUtils.getDebugException(exception) );
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

    public JmsEndpointConfig getEndpointConfig() {
        return this._endpointCfg;
    }

    @Override
    public Goid getJmsConnectionGoid() {
        return this._endpointCfg.getConnection().getGoid();
    }

    @Override
    public Goid getJmsEndpointGoid() {
        return this._endpointCfg.getEndpoint().getGoid();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (PROPERTY_ERROR_SLEEP.equals(evt.getPropertyName())) {
            String stringValue = (String) evt.getNewValue();
            setErrorSleepTime(stringValue);
        }
    }

    protected void fireConnected() {
        lastAuditErrorTime = 0L;
        fireEvent(new JMSEvent(this, Level.INFO, null,
                formatMessage(JmsMessages.INFO_EVENT_CONNECT_SUCCESS, identify())));
    }

    protected void fireConnectError(String message) {
        fireEvent(new JMSEvent(this, Level.WARNING,  null, formatMessage(
                        JmsMessages.INFO_EVENT_CONNECT_FAIL,
                        new Object[] {identify(), message}))
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

        private void  createReceiver() throws NamingException, JmsRuntimeException, JMSException, JmsConfigException {
            int noOfReveiver = DEFAULT_RECEIVER;
            String consumers = _endpointCfg.getConnection().properties().getProperty(JmsConnection.PROP_DEDICATED_CONSUMER_SIZE);
            if (consumers != null) {
                noOfReveiver = Integer.parseInt(consumers);
            }
            if (noOfReveiver <1) {
                noOfReveiver = DEFAULT_RECEIVER;
            }

            for (int i = 0; i < noOfReveiver; i++) {
                JmsBag jmsBag = resourceManager.borrowJmsBag(_endpointCfg);
                jmsBag.getMessageConsumer().setMessageListener(new JmsTask(_endpointCfg, jmsBag ));
                receivers.add(jmsBag);
            }
        }

        /**
         *
         */
        @Override
        public final void run() {

            final int[] oopses = {0};
            log(Level.INFO, JmsMessages.INFO_LISTENER_POLLING_START, identify());

            try {
                Message jmsMessage;
                while ( !isStop() ) {
                    try {

                        if (_endpointCfg.getEndpoint().isQueue()) {
                            //Process Queue

                            if (_jmsBag == null) {
                                //The main JmsBag is only for jndi lookup
                                _jmsBag = resourceManager.borrowJmsBag(_endpointCfg);
                                _jmsBag.closeSession();
                                createReceiver();
                                oopses[0] = 0;
                            } else {
                                Thread.sleep(RECEIVE_TIMEOUT);
                            }

                        } else {
                            //Process Topic
                            if (_jmsBag == null) {
                                _jmsBag = resourceManager.borrowJmsBag(_endpointCfg);
                                ensureConnectionStarted();
                                _jmsBag.getMessageConsumer().setMessageListener(new JmsTask(_endpointCfg, _jmsBag ));
                                _jmsBag.getConnection().setExceptionListener(new ExceptionListener() {
                                    @Override
                                    public void onException(JMSException e) {
                                        handleError(e, oopses);
                                    }
                                });
                                oopses[0] = 0;
                            } else {
                                Thread.sleep(RECEIVE_TIMEOUT);
                            }

                        }
                    } catch ( Throwable e ) {
                        handleError(e, oopses);
                    }
                }
            } finally {
                log(Level.INFO, JmsMessages.INFO_LISTENER_POLLING_STOP, identify());
                cleanup();
            }
        }
    }

    protected void handleError(Throwable e, int[] oopses) {
        if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
            log(Level.FINE, "JMS listener on {0} caught throwable: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return;
        }

        if (!ExceptionUtils.causedBy(e, RejectedExecutionException.class)) {
            log(Level.WARNING, formatMessage(
                    JmsMessages.WARN_LISTENER_RECEIVE_ERROR,
                    identify()),
                    ExceptionUtils.getDebugException(e));

            cleanup();
        }

        if ( ++oopses[0] < MAXIMUM_OOPSES ) {
            // sleep for a short period of time before retrying
            log(Level.FINE, "JMS listener on {0} sleeping for {1} milliseconds.", new Object[]{_endpointCfg.getEndpoint(), OOPS_RETRY});
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

