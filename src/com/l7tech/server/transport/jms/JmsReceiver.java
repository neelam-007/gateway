/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.logging.LogManager;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.jms.*;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Message processing runtime support for JMS messages.
 *
 * Publically Immutable but not thread-safe.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsReceiver implements ServerComponentLifecycle {
    private final JmsReplyType _replyType;
    private final Logger _logger = LogManager.getInstance().getSystemLogger();

    private JmsConnection _connection;

    private JmsEndpoint _inboundRequestEndpoint;
    private JmsEndpoint _outboundResponseEndpoint;
    private JmsEndpoint _failureEndpoint;

    private ConnectionFactory _connectionFactory;

    private Connection _inboundConnection;
    private Connection _outboundConnection;
    private Connection _failureConnection;

    private boolean _initialized = false;
    private InitialContext _context;
    private Queue _queue;
    private QueueSession _queueSession;
    private QueueConnection _queueConnection;

    /**
     * Complete constructor
     *
     * @param conn The {@link com.l7tech.common.transport.jms.JmsConnection} from which to receive messages
     * @param replyType A {@link com.l7tech.common.transport.jms.JmsReplyType} value indicating this receiver's
     * reply semantics
     * @param inbound The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     * @param outbound The {@link com.l7tech.common.transport.jms.JmsEndpoint} into which to submit replies
     */
    public JmsReceiver( JmsConnection conn, JmsReplyType replyType,
                        JmsEndpoint inbound, JmsEndpoint outbound,
                        JmsEndpoint failures ) {
        _connection = conn;
        _replyType = replyType;
        _inboundRequestEndpoint = inbound;
        _outboundResponseEndpoint = outbound;
        _failureEndpoint = failures;
    }

    /**
     * Convenience constructor for one-way or reply-to-same configurations.
     * <p>
     * Use this constructor when the replyType is either
     * {@link com.l7tech.common.transport.jms.JmsReplyType#NO_REPLY} or {@link com.l7tech.common.transport.jms.JmsReplyType#REPLY_TO_SAME},
     * since in those cases there is no meaningful outbound destination.
     *
     * @param conn The {@link com.l7tech.common.transport.jms.JmsConnection} from which to receive messages
     * @param replyType A {@link com.l7tech.common.transport.jms.JmsReplyType} value indicating this receiver's
     * reply semantics
     * @param inbound The {@link com.l7tech.common.transport.jms.JmsEndpoint} from which to receive requests
     */
    public JmsReceiver( JmsConnection conn, JmsReplyType replyType,
                        JmsEndpoint inbound ) {
        this( conn, replyType, inbound, null, null );
    }

    public String toString() {
        return "jmsReceiver:" + _connection.getName() + "/" + _inboundRequestEndpoint.getName();
    }

    public synchronized void init( ServerConfig config ) throws LifecycleException {
        Hashtable properties = new Hashtable();
        String classname = _connection.getInitialContextFactoryClassname();
        if ( classname != null && classname.length() > 0 )
            properties.put( InitialContext.INITIAL_CONTEXT_FACTORY, classname );

        String url = _connection.getJndiUrl();
        if ( url != null && url.length() > 0 )
            properties.put( InitialContext.PROVIDER_URL, url );

        try {
            _context = new InitialContext( properties );

            String qcfUrl = _connection.getQueueFactoryUrl();
            String dcfUrl = _connection.getDestinationFactoryUrl();

            if ( qcfUrl != null && qcfUrl.length() > 0 )
                _connectionFactory = (QueueConnectionFactory)_context.lookup( qcfUrl );

            if ( dcfUrl != null && dcfUrl.length() > 0 )
                _connectionFactory = (ConnectionFactory)_context.lookup( dcfUrl );

            if ( _connectionFactory == null ) {
                String msg = "No connection factory was configured for '" + _inboundRequestEndpoint.toString() + "'";
                _logger.log( Level.WARNING, msg );
                throw new LifecycleException( msg );
            }

            _queue = (Queue)_context.lookup( _inboundRequestEndpoint.getDestinationName() );

            _initialized = true;
        } catch ( NamingException e ) {
            _logger.log( Level.WARNING, "Caught NamingException initializing JMS context for '" + _inboundRequestEndpoint.toString() + "'", e );
            throw new LifecycleException( e.toString(), e );
        }
    }

    /**
     * Starts the receiver.
     */
    public synchronized void start() throws LifecycleException {
        if ( !_initialized ) throw new LifecycleException( "Can't start '" + _inboundRequestEndpoint.toString() + "', it has not been successfully initialized!" );

        String username = _inboundRequestEndpoint.getUsername();
        String password = _inboundRequestEndpoint.getPassword();

        if ( username == null || username.length() == 0 ) {
            username = _connection.getUsername();
            password = _connection.getPassword();
        }

        try {
            _inboundConnection = connect( username, password );

            if ( _inboundConnection instanceof QueueConnection ) {
                _queueConnection = (QueueConnection)_inboundConnection;
                _queueSession = _queueConnection.createQueueSession( false, // TODO parameterize
                                                         QueueSession.CLIENT_ACKNOWLEDGE );

                QueueReceiver receiver = _queueSession.createReceiver( _queue );
                receiver.setMessageListener( new JmsMessageListener( _queueSession, this ) );
            } else {
                _logger.severe( "Only queues are currently supported!" );
            }

            _inboundConnection.start();
        } catch ( JMSException e ) {
            throw new LifecycleException( e.getMessage(), e );
        }
    }

    private Connection connect( String username, String password ) throws JMSException {
        Connection conn = null;

        if ( _connectionFactory instanceof QueueConnectionFactory )
            conn = ((QueueConnectionFactory)_connectionFactory).createQueueConnection( username, password );

        if ( conn == null &&  _connectionFactory != null )
            conn = _connectionFactory.createConnection( username, password );

        if ( conn != null ) return conn;

        String msg = "No connection factories were able to establish a connection to " + _inboundRequestEndpoint.toString();
        _logger.warning( msg );
        throw new JMSException( msg );
    }

    /**
     * Stops the receiver, e.g. temporarily.
     */
    public synchronized void stop() throws LifecycleException {
    }

    /**
     * Closes the receiver, and any resources it may have allocated.  Note that
     * a receiver that has been closed cannot be restarted.
     *
     * Nulls all references to runtime objects.
     */
    public synchronized void close() throws LifecycleException {
        _initialized = false;

        _connection = null;
        _inboundRequestEndpoint = null;
        _failureEndpoint = null;

        _connectionFactory = null;
    }
}
