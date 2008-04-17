/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.TimeUnit;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.JMSEvent;
import org.springframework.context.ApplicationContext;

import javax.jms.*;
import javax.jms.IllegalStateException;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Message processing runtime support for JMS messages.
 * <p/>
 * Publically Immutable but not thread-safe.
 */
public class JmsReceiver implements PropertyChangeListener {
    // Statics
    private static final Logger _logger = Logger.getLogger(JmsReceiver.class.getName());
    private static final String PROPERTY_ERROR_SLEEP = "ioJmsErrorSleep";    
    private static final String PROPERTY_MAX_SIZE = "ioJmsMessageMaxBytes";
    private static final int MAXIMUM_OOPSES = 5;
    /** Set to five seconds so that the uninterruptible (!) poll doesn't pause server shutdown for too long. */
    private static final long RECEIVE_TIMEOUT = 5 * 1000;
    private static final long SHUTDOWN_TIMEOUT = 7 * 1000;
    public static final int OOPS_RETRY = 5000; // Five seconds
    public static final int DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    public static final int MIN_OOPS_SLEEP = 10 * 1000; // 10 seconds
    public static final int MAX_OOPS_SLEEP = TimeUnit.DAYS.getMultiplier(); // 24 hours
    public static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 mins;
    public static final int DEFAULT_MAX_SIZE = 5242880; 

    // Persistence stuff
    private final JmsReplyType _replyType;
    private final JmsConnection _connection;
    private final JmsEndpoint _inboundRequestEndpoint;
    private final JmsPropertyMapper _jmsPropertyMapper;

    // Runtime stuff
    private final JmsRequestHandler _handler;
    private final MessageLoop _loop;
    private final Object syncRecv = new Object();
    private final AtomicInteger oopsSleep = new AtomicInteger(DEFAULT_OOPS_SLEEP);
    private final AtomicInteger maxMessageSize = new AtomicInteger(DEFAULT_MAX_SIZE);
    private final ApplicationContext applicationContext;

    /**
     * Complete constructor
     *
     * @param inbound   The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     * @param replyType A {@link com.l7tech.common.transport.jms.JmsReplyType} value indicating this receiver's
     */
    public JmsReceiver(final JmsConnection connection,
                       final JmsEndpoint inbound,
                       final JmsReplyType replyType,
                       final JmsPropertyMapper jmsPropertyMapper,
                       final ApplicationContext applicationContext) {
        this._connection = connection;
        this._inboundRequestEndpoint = inbound;
        if (replyType == null) {
            this._replyType = JmsReplyType.AUTOMATIC;
        } else {
            this._replyType = replyType;
        }
        this._jmsPropertyMapper = jmsPropertyMapper;
        this.applicationContext = applicationContext;


        if ( applicationContext == null ) {
            _logger.warning("Application context set to null");
        }

        _logger.info( "Initializing " + toString() + "..." );
        _handler = new JmsRequestHandler(applicationContext);
        _loop = new MessageLoop();
        _logger.info( toString() + " initialized successfully" );

        String stringValue = ServerConfig.getInstance().getProperty(PROPERTY_ERROR_SLEEP);
        setErrorSleepTime(stringValue);

        String stringValueMax = ServerConfig.getInstance().getProperty(PROPERTY_MAX_SIZE);
        setMessageMaxSize(stringValueMax);
    }

    /**
     * Convenience constructor for automatic, one-way or reply-to-same configurations.
     * <p/>
     * Use this constructor when the replyType is either
     * {@link com.l7tech.common.transport.jms.JmsReplyType#AUTOMATIC},
     * {@link com.l7tech.common.transport.jms.JmsReplyType#NO_REPLY} or
     *
     * @param inbound The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     */
    public JmsReceiver(final JmsConnection connection,
                       final JmsEndpoint inbound,
                       final JmsPropertyMapper jmsPropertyMapper,
                       final ApplicationContext applicationContext) {
        this( connection, inbound, inbound.getReplyType(), jmsPropertyMapper, applicationContext);
    }

    @Override
    public String toString() {
        return "jmsReceiver:" + getDisplayName();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (PROPERTY_ERROR_SLEEP.equals(evt.getPropertyName())) {
            String stringValue = (String) evt.getNewValue();
            setErrorSleepTime(stringValue);
        }
        else if (PROPERTY_MAX_SIZE.equals(evt.getPropertyName())) {
            String stringValue = (String) evt.getNewValue();
            setMessageMaxSize(stringValue);
        }
    }

    JmsConnection getConnection() {
        return _connection;
    }

    JmsEndpoint getInboundRequestEndpoint() {
        return _inboundRequestEndpoint;
    }

    /**
     * Finds the proper JMS destination to send a response to.
     * <p/>
     * <b>This method can return null</b> under some circumstances:
     * <ul>
     * <li>If the ReplyType is {@link com.l7tech.common.transport.jms.JmsReplyType#NO_REPLY};
     * <li>If the ReplyType is {@link com.l7tech.common.transport.jms.JmsReplyType#AUTOMATIC} but the
     * incoming request specified no ReplyTo queue.
     *
     * @return The JMS Destination to which responses should be sent, possibly null.
     * @throws JMSException
     */
    Destination getOutboundResponseDestination(Message request, JmsBag bag) throws JMSException, NamingException {
        if (_replyType == JmsReplyType.NO_REPLY) {
            _logger.finer("Returning NO_REPLY (null) for '" + toString() + "'");
            return null;
        } else {
            if (_replyType == JmsReplyType.AUTOMATIC) {
                _logger.finer("Returning AUTOMATIC '" + request.getJMSReplyTo() +
                  "' for '" + toString() + "'");
                return request.getJMSReplyTo();
            } else if (_replyType == JmsReplyType.REPLY_TO_OTHER) {
                // use bag's jndi context to lookup the the destination and create the sender
                Context jndiContext = bag.getJndiContext();
                String replyToQueueName = _inboundRequestEndpoint.getReplyToQueueName();
                _logger.fine("looking up destination name " + replyToQueueName);
                return (Destination)jndiContext.lookup(replyToQueueName);
            } else {
                String msg = "Unknown JmsReplyType " + _replyType.toString();
                _logger.severe(msg);
                throw new IllegalStateException(msg);
            }
        }
    }

    int getMessageMaxSize() {
        return this.maxMessageSize.get();
    }

    /**
     * Starts the receiver.
     */
    public void start() throws LifecycleException {
        synchronized(syncRecv) {
            _logger.info( "Starting " + toString() + "..." );

            try {
                _loop.start();
            } catch (JMSException e) {
                throw new LifecycleException(e.getMessage(), e);
            } catch (NamingException e) {
                throw new LifecycleException(e.getMessage(), e);
            } catch ( JmsConfigException e ) {
                throw new LifecycleException(e.getMessage(), e);
            }
        }
    }

    /**
     * Stops the receiver, e.g. temporarily.
     */
    public void stop() {
        synchronized(syncRecv) {
            _logger.info( "Stopping " + toString() + "..." );
            _loop.stop();
        }
    }

    /**
     * Closes the receiver, and any resources it may have allocated.  Note that
     * a receiver that has been closed cannot be restarted.
     * <p/>
     * Nulls all references to runtime objects.
     */
    public void ensureStopped() {
        synchronized(syncRecv) {
            _loop.ensureStopped();
        }
    }
    
    private String getDisplayName() {
        return getConnection().getJndiUrl()  + "/" + getConnection().getName();
    }

    private void setErrorSleepTime(String stringValue) {
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

    private void setMessageMaxSize(String stringValue) {
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
        maxMessageSize.set(newMaxSize);
    }

    private class MessageLoop implements Runnable {
        private MessageLoop() {
            _thread = new Thread( this, toString() );
            _thread.setDaemon( true );
        }

        private JmsBag getBag() throws JmsConfigException, JMSException, NamingException {
            synchronized(sync) {
                if ( _bag == null ) {
                    _logger.finest( "Getting new JmsBag" );
                    boolean transactional = _inboundRequestEndpoint.getAcknowledgementType()==JmsAcknowledgementType.ON_COMPLETION;
                    _bag = JmsUtil.connect(
                            _connection,
                            _inboundRequestEndpoint.getPasswordAuthentication(),
                            _jmsPropertyMapper,
                            transactional,
                            Session.CLIENT_ACKNOWLEDGE);
                }
                return _bag;
            }
        }

        private QueueReceiver getConsumer() throws JMSException, NamingException, JmsConfigException {
            synchronized(sync) {
                if ( _consumer == null ) {
                    _logger.finest( "Getting new MessageConsumer" );
                    boolean ok = false;
                    String message = null;
                    try {
                        JmsBag bag = getBag();
                        Session s = bag.getSession();
                        if ( !(s instanceof QueueSession) ) {
                            message = "Only QueueSessions are supported";
                            throw new JmsConfigException(message);
                        }
                        QueueSession qs = (QueueSession)s;
                        Queue q = getQueue();
                        _consumer = qs.createReceiver( q );
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
                        if (!ok) {
                            fireConnectError(message);
                        }
                    }
                }
                return _consumer;
            }
        }

        private Queue getQueue() throws NamingException, JmsConfigException, JMSException {
            synchronized(sync) {
                if ( _queue == null ) {
                    _logger.finest( "Getting new Queue" );
                    JmsBag bag = getBag();
                    Context context = bag.getJndiContext();
                    String qname = _inboundRequestEndpoint.getDestinationName();
                    _queue = (Queue)context.lookup( qname );
                }
                return _queue;
            }
        }

        private QueueSender getFailureSender() throws JMSException, NamingException, JmsConfigException {
            synchronized(sync) {
                if ( _failureSender == null &&
                        _inboundRequestEndpoint.getAcknowledgementType()==JmsAcknowledgementType.ON_COMPLETION &&
                        _inboundRequestEndpoint.getFailureDestinationName()!=null ) {
                    _logger.finest( "Getting new MessageSender" );
                    boolean ok = false;
                    String message = null;
                    try {
                        JmsBag bag = getBag();
                        Session s = bag.getSession();
                        if ( !(s instanceof QueueSession) ) {
                            message = "Only QueueSessions are supported";
                            throw new JmsConfigException(message);
                        }
                        QueueSession qs = (QueueSession)s;
                        Queue q = getFailureQueue();
                        _failureSender = qs.createSender( q );
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
                        if (!ok) {
                            fireConnectError(message);
                        }
                    }
                }
                return _failureSender;
            }
        }

        private Queue getFailureQueue() throws NamingException, JmsConfigException, JMSException {
            synchronized(sync) {
                if ( _failureQueue == null ) {
                    _logger.finest( "Getting new FailureQueue" );
                    JmsBag bag = getBag();
                    Context context = bag.getJndiContext();
                    String qname = _inboundRequestEndpoint.getFailureDestinationName();
                    _failureQueue = (Queue)context.lookup( qname );
                }
                return _failureQueue;
            }
        }

        private void ensureConnectionStarted() throws NamingException, JmsConfigException, JMSException {
            synchronized(sync) {
                boolean ok = false;
                String message = null;
                try {
                    JmsBag bag = getBag();
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
            StringBuffer s = new StringBuffer( "MessageLoop-" );
            s.append( _connection.getOid() );
            s.append( "/" );
            s.append( _inboundRequestEndpoint.getDestinationName() );
            return s.toString();
        }

        void start() throws JMSException, NamingException, JmsConfigException {
            synchronized(sync) {
                _logger.fine( "Starting " + toString() );
                _thread.start();
                _logger.fine( "Started " + toString() );
            }
        }

        void stop() {
            synchronized(sync) {
                _logger.fine( "Stopping " + toString() );
                _stop = true;
                lastStopRequestedTime = System.currentTimeMillis();
            }
        }

        private void cleanup() {
            synchronized(sync) {
                _logger.info( "Closing JMS connection..." );
                if ( _consumer != null ) {
                    try {
                        _consumer.close();
                    } catch ( JMSException e ) {
                        _logger.log( Level.INFO, "Caught JMSException during cleanup", e );
                    }
                    _consumer = null;
                }

                if ( _failureSender != null ) {
                    try {
                        _failureSender.close();
                    } catch ( JMSException e ) {
                        _logger.log( Level.INFO, "Caught JMSException during cleanup", e );
                    }
                    _failureSender = null;
                }

                _queue = null;
                _failureQueue = null;
                _started = false;

                if ( _bag != null ) {
                    // this will close the session and cause rollback if transacted
                    _bag.close();
                    _bag = null;
                }
            }
        }

        private boolean isStop() {
            synchronized(sync) {
                return _stop;
            }
        }

        public void run() {
            int oopses = 0;

            _logger.info( "Starting JMS poller on " + _inboundRequestEndpoint.getDestinationName() );
            try {
                while ( !isStop() ) {
                    try {
//                        _logger.finest( "Polling for a message on " + _inboundRequestEndpoint.getDestinationName() );
                        QueueReceiver receiver = getConsumer();
                        QueueSender sender = getFailureSender();
                        ensureConnectionStarted();
                        boolean isTransacted = _inboundRequestEndpoint.getAcknowledgementType()==JmsAcknowledgementType.ON_COMPLETION;

                        Message jmsMessage = receiver.receive( RECEIVE_TIMEOUT );
                        if ( jmsMessage != null && !isStop() ) {
                            if ( !isTransacted ) jmsMessage.acknowledge();
                            _logger.fine( "Received a message on " + _inboundRequestEndpoint.getDestinationName() );
                            oopses = 0;
                            // todo support concurrent JMS messages some day
                            _handler.onMessage( JmsReceiver.this, getBag(), isTransacted, sender, jmsMessage );
                        }
                    } catch ( Throwable e ) {
                        if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
                            continue;
                        }
                        _logger.log( Level.WARNING,
                                     "Unable to receive message from JMS endpoint " +
                                     _inboundRequestEndpoint, e );

                        cleanup();

                        if ( ++oopses < MAXIMUM_OOPSES ) {
                            try {
                                Thread.sleep(OOPS_RETRY);
                            } catch ( InterruptedException e1 ) {
                                _logger.info( "Interrupted during retry interval" );
                            }
                        } else {
                            int sleepTime = oopsSleep.get();
                            _logger.warning( "Too many errors (" + MAXIMUM_OOPSES + ") - listener for JMS endpoint " + _inboundRequestEndpoint + " will try again in " + sleepTime + "ms" );
                            try {
                                Thread.sleep(sleepTime);
                            } catch ( InterruptedException e1 ) {
                                _logger.info( "Interrupted during sleep interval" );
                            }
                        }
                    }
                }
            } finally {
                _logger.info( "Stopped JMS poller on " + _inboundRequestEndpoint.getDestinationName() );
                cleanup();
            }
        }

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
                _logger.warning( this + " did not shutdown on request." );
            }
        }

        private void fireConnected() {
            lastAuditErrorTime = 0L;
            fireEvent(new JMSEvent(this, Level.INFO, null, "Connected to '"+getDisplayName()+"'"));
        }

        private void fireConnectError(String message) {
            fireEvent(new JMSEvent(this, Level.WARNING,  null, "Error connecting to '"+getDisplayName()+"'; " + message));
        }

        private void fireEvent(JMSEvent event) {
            if (applicationContext != null) {
                long timeNow = System.currentTimeMillis();
                if ((lastAuditErrorTime+OOPS_AUDIT) < timeNow) {
                    lastAuditErrorTime = timeNow;
                    applicationContext.publishEvent(event);
                } else {
                    _logger.info("Not publishing event due to recent failure.");
                }

            } else {
                _logger.warning("Event not published, message is: " + event.getMessage());
            }
        }

        // JMS stuff
        private boolean _started = false;
        private JmsBag _bag;
        private QueueReceiver _consumer;
        private Queue _queue;
        private QueueSender _failureSender;
        private Queue _failureQueue;

        // Runtime stuff
        private boolean _stop = false;
        private final Object sync = new Object();
        private final Thread _thread;
        private long lastStopRequestedTime;
        private long lastAuditErrorTime = 0L;
    }
}
