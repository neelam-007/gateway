/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.logging.LogManager;
import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message processing runtime support for JMS messages.
 * <p/>
 * Publically Immutable but not thread-safe.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsReceiver implements ServerComponentLifecycle {
    // Statics
    private static final Logger _logger = LogManager.getInstance().getSystemLogger();
    private static final int MAXIMUM_OOPSES = 5;
    private static final long RECEIVE_TIMEOUT = 5000L;

    // Persistence stuff
    private final JmsReplyType _replyType;
    private final JmsConnection _connection;
    private final JmsEndpoint _inboundRequestEndpoint;
    private final JmsEndpoint _outboundResponseEndpoint;
    private final JmsEndpoint _failureEndpoint;

    // JMS stuff
    private JmsBag _bag;
    private Queue _jmsInboundQueue;
    private Queue _jmsOutboundQueue;
    private Queue _jmsFailureQueue;

    // Runtime stuff
    private boolean _initialized = false;

    /**
     * Complete constructor
     *
     * @param inbound   The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     * @param replyType A {@link com.l7tech.common.transport.jms.JmsReplyType} value indicating this receiver's
     *                  reply semantics
     * @param outbound  The {@link com.l7tech.common.transport.jms.JmsEndpoint} into which replies should be sent
     * @param failures  The {@link com.l7tech.common.transport.jms.JmsEndpoint} into which failures should be sent
     */
    public JmsReceiver( JmsConnection connection, JmsEndpoint inbound, JmsReplyType replyType,
                       JmsEndpoint outbound, JmsEndpoint failures) {
        _connection = connection;
        _inboundRequestEndpoint = inbound;
        if (replyType == null) replyType = JmsReplyType.AUTOMATIC;
        _replyType = replyType;
        _outboundResponseEndpoint = outbound;
        _failureEndpoint = failures;
    }

    /**
     * Convenience constructor for automatic, one-way or reply-to-same configurations.
     * <p/>
     * Use this constructor when the replyType is either
     * {@link com.l7tech.common.transport.jms.JmsReplyType#AUTOMATIC},
     * {@link com.l7tech.common.transport.jms.JmsReplyType#NO_REPLY} or
     * {@link com.l7tech.common.transport.jms.JmsReplyType#REPLY_TO_SAME}.
     *
     * @param replyType A {@link com.l7tech.common.transport.jms.JmsReplyType} value indicating this receiver's
     *                  reply semantics
     * @param inbound   The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     */
    public JmsReceiver( JmsConnection connection, JmsEndpoint inbound, JmsReplyType replyType) {
        this( connection, inbound, replyType, null, null);
    }

    /**
     * Convenience constructor for automatic, one-way or reply-to-same configurations.
     * <p/>
     * Use this constructor when the replyType is either
     * {@link com.l7tech.common.transport.jms.JmsReplyType#AUTOMATIC},
     * {@link com.l7tech.common.transport.jms.JmsReplyType#NO_REPLY} or
     * {@link com.l7tech.common.transport.jms.JmsReplyType#REPLY_TO_SAME}.
     *
     * @param inbound The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     */
    public JmsReceiver(JmsConnection connection, JmsEndpoint inbound) {
        this( connection, inbound, inbound.getReplyType());
    }

    public String toString() {
        return "jmsReceiver:" + getConnection().getName() + "/" + _inboundRequestEndpoint.getName();
    }

    JmsConnection getConnection() {
        return _connection;
    }

    JmsEndpoint getInboundRequestEndpoint() {
        return _inboundRequestEndpoint;
    }

    JmsEndpoint getOutboundResponseEndpoint() {
        return _outboundResponseEndpoint;
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
     * @param request
     * @param response
     * @return The JMS Destination to which responses should be sent, possibly null.
     * @throws JMSException
     */
    Destination getOutboundResponseDestination(Message request, Message response) throws JMSException {
        if (_replyType == JmsReplyType.NO_REPLY) {
            _logger.finer("Returning NO_REPLY (null) for '" + toString() + "'");
            return null;
        } else {
            if (_replyType == JmsReplyType.AUTOMATIC) {
                _logger.finer("Returning AUTOMATIC '" + request.getJMSReplyTo() +
                  "' for '" + toString() + "'");
                return request.getJMSReplyTo();
            } else if (_replyType == JmsReplyType.REPLY_TO_SAME) {
                _logger.finer("Returning REPLY_TO_SAME '" + _inboundRequestEndpoint.getDestinationName() +
                  "' for '" + toString() + "'");
                return _jmsInboundQueue;
            } else if (_replyType == JmsReplyType.REPLY_TO_OTHER) {
                _logger.finer("Returning REPLY_TO_OTHER '" + _inboundRequestEndpoint.getDestinationName() +
                  "' for '" + toString() + "'");
                return _jmsOutboundQueue;
            } else {
                String msg = "Unknown JmsReplyType " + _replyType.toString();
                _logger.severe(msg);
                throw new java.lang.IllegalStateException(msg);
            }
        }
    }

    JmsEndpoint getFailureEndpoint() {
        return _failureEndpoint;
    }

    JmsReplyType getReplyType() {
        return _replyType;
    }

    Queue getJmsFailureQueue() {
        return _jmsFailureQueue;
    }

    /**
     * Initializes the JMS receiver.
     * @param config
     * @throws LifecycleException
     */
    public synchronized void init(ComponentConfig config) throws LifecycleException {
        _logger.info( "Initializing " + toString() + "..." );
        try {
            _bag = JmsUtil.connect( _connection, _inboundRequestEndpoint.getPasswordAuthentication() );

            Context jndiContext = _bag.getJndiContext();
            _jmsInboundQueue = (Queue)jndiContext.lookup(_inboundRequestEndpoint.getDestinationName());

            if (_outboundResponseEndpoint != null) {
                _logger.fine( "Using " + _outboundResponseEndpoint.getDestinationName() + " for outbound response messages" );
                String dname = _outboundResponseEndpoint.getDestinationName();
                _jmsOutboundQueue = (Queue)jndiContext.lookup(dname);
            }

            if (_failureEndpoint != null) {
                _logger.fine( "Using " + _outboundResponseEndpoint.getDestinationName() + " for failure messages" );
                String dname = _failureEndpoint.getDestinationName();
                _jmsFailureQueue = (Queue)jndiContext.lookup(dname);
            }

            _initialized = true;
            _logger.info( toString() + " initialized successfully" );
        } catch (NamingException e) {
            _logger.log(Level.WARNING, "Caught NamingException initializing JMS context for '" + _inboundRequestEndpoint.toString() + "'", e);
            throw new LifecycleException(e.toString(), e);
        } catch ( JMSException e ) {
            _logger.log(Level.WARNING, "Caught JMSException initializing JMS context for '" + _inboundRequestEndpoint.toString() + "'", e);
            throw new LifecycleException(e.toString(), e);
        } catch ( JmsConfigException e ) {
            _logger.log(Level.WARNING, "Caught JMSException initializing JMS context for '" + _inboundRequestEndpoint.toString() + "'", e);
            throw new LifecycleException(e.toString(), e);
        } finally {
            if ( !_initialized && _bag != null ) _bag.close();
        }
    }

    /**
     * Starts the receiver.
     */
    public synchronized void start() throws LifecycleException {
        _logger.info( "Starting " + toString() + "..." );

        if (!_initialized) throw new LifecycleException("Can't start '" + _inboundRequestEndpoint.toString() + "', it has not been successfully initialized!");

        try {
            _loop = new MessageLoop(_bag);
            _loop.start();
        } catch (JMSException e) {
            throw new LifecycleException(e.getMessage(), e);
        } catch (NamingException e) {
            throw new LifecycleException(e.getMessage(), e);
        }
    }

    private class MessageLoop implements Runnable {
        MessageLoop( JmsBag bag ) throws JMSException, NamingException {
            _bag = bag;
            _thread = new Thread( this, "MessageLoop_" + _connection.getOid() + "/" +
                                        _inboundRequestEndpoint.getOid() );
            Session session = _bag.getSession();
            if ( session instanceof QueueSession ) {
                QueueSession queueSession = (QueueSession)session;
                _queue = (Queue)bag.getJndiContext().lookup( _inboundRequestEndpoint.getDestinationName() );
                _consumer = queueSession.createReceiver( _queue );
            } else
                throw new IllegalArgumentException("Only QueueSession is supported");
        }

        public void start() throws JMSException {
            _bag.getConnection().start();
            _thread.start();
        }

        public void stop() {
            _stop = true;
            _thread.interrupt();
            try {
                _thread.join();
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
            }
        }

        public void run() {
            int oopses = 0;

            _logger.info( "Starting JMS poller on " + _inboundRequestEndpoint.getDestinationName() );
            try {
                while ( !_stop ) {
                    try {
                        Message jmsMessage = _consumer.receive( RECEIVE_TIMEOUT );
                        if ( jmsMessage != null ) {
                            oopses = 0;
                            // todo support concurrent JMS messages some day
                            JmsRequestHandler handler = new JmsRequestHandler();
                            handler.onMessage( JmsReceiver.this, _bag, jmsMessage );
                        }
                    } catch ( Throwable e ) {
                        _logger.log( Level.WARNING,
                                     "Unable to receive message from JMS endpoint " + _inboundRequestEndpoint,
                                     e );
                        if ( oopses++ > MAXIMUM_OOPSES ) {
                            _logger.severe( "Too many errors - shutting down listener for JMS endpoint " + _inboundRequestEndpoint );
                            return;
                        }
                    }
                }
            } finally {
                _logger.info( "Stopping JMS poller on " + _inboundRequestEndpoint.getDestinationName() );
            }
        }

        public void close() {
            stop();
            if ( _bag != null ) _bag.close();
        }

        // JMS stuff
        private QueueReceiver _consumer;
        private Queue _queue;

        // Runtime stuff
        private volatile boolean _stop = false;
        private Thread _thread;
    }

    /**
     * Stops the receiver, e.g. temporarily.
     */
    public synchronized void stop() throws LifecycleException {
        if ( _loop != null ) {
            _loop.stop();
        }
    }

    /**
     * Closes the receiver, and any resources it may have allocated.  Note that
     * a receiver that has been closed cannot be restarted.
     * <p/>
     * Nulls all references to runtime objects.
     */
    public synchronized void close() throws LifecycleException {
        _initialized = false;
        if ( _loop == null )
            _bag.close();
        else
            _loop.close(); // This will close the bag too

    }

    private MessageLoop _loop;

}
