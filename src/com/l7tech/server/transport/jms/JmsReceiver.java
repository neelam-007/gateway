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
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.IllegalStateException;

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
    public static final int OOPS_RETRY = 5000; // Five seconds
    public static final int OOPS_SLEEP = 1 * 60 * 1000; // One minute

    // Persistence stuff
    private final JmsReplyType _replyType;
    private final JmsConnection _connection;
    private final JmsEndpoint _inboundRequestEndpoint;

    // JMS stuff

    // Runtime stuff
    private boolean _initialized = false;

    /**
     * Complete constructor
     *
     * @param inbound   The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     * @param replyType A {@link com.l7tech.common.transport.jms.JmsReplyType} value indicating this receiver's
     *                  reply semantics
     */
    public JmsReceiver( JmsConnection connection, JmsEndpoint inbound, JmsReplyType replyType ) {
        _connection = connection;
        _inboundRequestEndpoint = inbound;
        if (replyType == null) replyType = JmsReplyType.AUTOMATIC;
        _replyType = replyType;
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
            } else if (_replyType == JmsReplyType.REPLY_TO_OTHER) {
                String msg = "REPLY_TO_OTHER not currently supported";
                _logger.severe(msg);
                throw new IllegalStateException(msg);
/*
                _logger.finer("Returning REPLY_TO_OTHER '" + _inboundRequestEndpoint.getDestinationName() +
                  "' for '" + toString() + "'");
                return getJmsOutboundQueue();
*/
            } else {
                String msg = "Unknown JmsReplyType " + _replyType.toString();
                _logger.severe(msg);
                throw new IllegalStateException(msg);
            }
        }
    }

    JmsReplyType getReplyType() {
        return _replyType;
    }

    /**
     * Initializes the JMS receiver.
     * @param config
     * @throws LifecycleException
     */
    public synchronized void init(ComponentConfig config) throws LifecycleException {
        _logger.info( "Initializing " + toString() + "..." );
        try {
            _initialized = true;
            _loop = new MessageLoop();
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
            if ( !_initialized && _loop != null ) _loop.close();
        }
    }


    /**
     * Starts the receiver.
     */
    public synchronized void start() throws LifecycleException {
        _logger.info( "Starting " + toString() + "..." );

        if (!_initialized) throw new LifecycleException("Can't start '" + _inboundRequestEndpoint.toString() + "', it has not been successfully initialized!");

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

    private class MessageLoop implements Runnable {
        MessageLoop() throws JMSException, NamingException, JmsConfigException {
            Session session = getBag().getSession();
            if ( !(session instanceof QueueSession) )
                throw new IllegalArgumentException("Only QueueSession is supported");

            _thread = new Thread( this, toString() );
            _thread.setDaemon( true );
        }

        private synchronized JmsBag getBag() throws JmsConfigException, JMSException, NamingException {
            if ( _bag == null ) {
                _bag = JmsUtil.connect( _connection, _inboundRequestEndpoint.getPasswordAuthentication() );
            }
            return _bag;
        }

        private synchronized QueueReceiver getConsumer() throws JMSException, NamingException, JmsConfigException {
            if ( _consumer == null ) {
                _consumer = ((QueueSession)getBag().getSession()).createReceiver( getQueue() );
            }
            return _consumer;
        }

        private synchronized Queue getQueue() throws NamingException, JmsConfigException, JMSException {
            if ( _queue == null ) {
                _queue = (Queue)getBag().getJndiContext().lookup( _inboundRequestEndpoint.getDestinationName() );
            }
            return _queue;
        }

        public String toString() {
            StringBuffer s = new StringBuffer( "MessageLoop-" );
            s.append( _connection.getOid() );
            s.append( "/" );
            s.append( _inboundRequestEndpoint.getDestinationName() );
            return s.toString();
        }

        synchronized void start() throws JMSException, NamingException, JmsConfigException {
            _logger.fine( "Starting " + toString() );
            getBag().getConnection().start();
            _thread.start();
            _logger.fine( "Started " + toString() );
        }

        synchronized void stop() {
            _logger.fine( "Stopping " + toString() );
            _stop = true;
            _thread.interrupt();
            try {
                _thread.join();
            } catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
            } finally {
                cleanup();
                _logger.fine( "Stopped " + toString() );
            }
        }

        private synchronized void cleanup() {
            if ( _consumer != null ) {
                try {
                    _consumer.close();
                    _consumer = null;
                } catch ( JMSException e ) {
                    _logger.log( Level.INFO, "Caught JMSException during cleanup", e );
                }
            }

            _queue = null;

            if ( _bag != null ) {
                _bag.close();
                _bag = null;
            }
        }

        public void run() {
            int oopses = 0;

            _logger.info( "Starting JMS poller on " + _inboundRequestEndpoint.getDestinationName() );
            try {
                while ( !_stop ) {
                    try {
//                        _logger.finest( "Polling for a message on " + _inboundRequestEndpoint.getDestinationName() );
                        Message jmsMessage = getConsumer().receive( RECEIVE_TIMEOUT );
                        if ( jmsMessage != null ) {
                            _logger.fine( "Received a message on " + _inboundRequestEndpoint.getDestinationName() );
                            oopses = 0;
                            // todo support concurrent JMS messages some day
                            _handler.onMessage( JmsReceiver.this, getBag(), jmsMessage );
                        }
                    } catch ( Throwable e ) {
                        _logger.log( Level.WARNING,
                                     "Unable to receive message from JMS endpoint " +
                                     _inboundRequestEndpoint, e );

                        cleanup();

                        if ( ++oopses < MAXIMUM_OOPSES ) {
                            try {
                                Thread.sleep(OOPS_RETRY);
                            } catch ( InterruptedException e1 ) {
                                _logger.info( "Interrupted during retry interval" );
                                Thread.currentThread().interrupt();
                            }
                        } else {
                            _logger.warning( "Too many errors (" + MAXIMUM_OOPSES + ") - listener for JMS endpoint " + _inboundRequestEndpoint + " will try again in " + OOPS_SLEEP + "ms" );
                            try {
                                Thread.sleep(OOPS_SLEEP);
                            } catch ( InterruptedException e1 ) {
                                _logger.info( "Interrupted during sleep interval" );
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            } finally {
                _logger.info( "Stopping JMS poller on " + _inboundRequestEndpoint.getDestinationName() );
            }
        }

        public void close() {
            _logger.fine( "Closing " + toString() );
            stop();
            if ( _bag != null ) _bag.close();
            _logger.fine( "Closed " + toString() );
        }

        // JMS stuff
        private JmsBag _bag;
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
        if ( _loop != null ) _loop.stop();
    }

    /**
     * Closes the receiver, and any resources it may have allocated.  Note that
     * a receiver that has been closed cannot be restarted.
     * <p/>
     * Nulls all references to runtime objects.
     */
    public synchronized void close() throws LifecycleException {
        _initialized = false;
        if ( _loop != null ) _loop.close();
    }

    private final JmsRequestHandler _handler = new JmsRequestHandler();
    private MessageLoop _loop;
}
