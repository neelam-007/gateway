/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;
import EDU.oswego.cs.dl.util.concurrent.Semaphore;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.logging.LogManager;
import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
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
    private final Logger _logger = LogManager.getInstance().getSystemLogger();

    // Persistent stuff
    private final JmsReplyType _replyType;
    private final JmsConnection _connection;
    private final JmsEndpoint _inboundRequestEndpoint;
    private final JmsEndpoint _outboundResponseEndpoint;
    private final JmsEndpoint _failureEndpoint;

    // Runtime stuff
    private FIFOSemaphore _fifo;
    private boolean _initialized = false;

    // JMS stuff
    private InitialContext _jmsContext;
    private ConnectionFactory _jmsConnectionFactory;
    private Connection _jmsInboundConnection;
    private Queue _jmsInboundQueue;
    private Queue _jmsOutboundQueue;
    private Queue _jmsFailureQueue;
    private QueueSession _jmsQueueSession;
    private QueueConnection _jmsQueueConnection;

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

    Semaphore getSemaphore() {
        return _fifo;
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
     * <b>Side Effect</b>: Sets the response's JMSCorrelationID if the replyType for this
     * receiver is anything other than {@link com.l7tech.common.transport.jms.JmsReplyType#NO_REPLY}.
     * <p/>
     * <b>This method can return null</b> under some circumstances:
     * <ul>
     * <li>If the ReplyType is {@link com.l7tech.common.transport.jms.JmsReplyType#NO_REPLY};
     * <li>If the ReplyType is {@link com.l7tech.common.transport.jms.JmsReplyType#AUTOMATIC} but the
     * inc
     *
     * @param request
     * @param response
     * @return The JMS Destination to which responses should be sent, possibly null.
     * @throws JMSException
     */
    Destination getOutboundResponseDestination(Message request, Message response) throws JMSException {
        if (_replyType == JmsReplyType.NO_REPLY) {
            _logger.fine("Returning NO_REPLY (null) for '" + toString() + "'");
            return null;
        } else {
            response.setJMSCorrelationID(request.getJMSCorrelationID());

            if (_replyType == JmsReplyType.AUTOMATIC) {
                _logger.fine("Returning AUTOMATIC '" + request.getJMSReplyTo() +
                  "' for '" + toString() + "'");
                return request.getJMSReplyTo();
            } else if (_replyType == JmsReplyType.REPLY_TO_SAME) {
                _logger.fine("Returning REPLY_TO_SAME '" + _inboundRequestEndpoint.getDestinationName() +
                  "' for '" + toString() + "'");
                return _jmsInboundQueue;
            } else if (_replyType == JmsReplyType.REPLY_TO_OTHER) {
                _logger.fine("Returning REPLY_TO_OTHER '" + _inboundRequestEndpoint.getDestinationName() +
                  "' for '" + toString() + "'");
                return _jmsOutboundQueue;
            } else {
                String msg = "Unknown JmsReplyType " + _replyType.toString();
                _logger.severe(msg);
                throw new RuntimeException(msg);
            }
        }

    }

    JmsEndpoint getFailureEndpoint() {
        return _failureEndpoint;
    }

    JmsReplyType getReplyType() {
        return _replyType;
    }

    public synchronized void init(ComponentConfig config) throws LifecycleException {
        _logger.info( "Initializing " + toString() + "..." );
        Hashtable properties = new Hashtable();
        JmsConnection conn = getConnection();
        String classname = conn.getInitialContextFactoryClassname();
        if (classname != null && classname.length() > 0)
            properties.put(InitialContext.INITIAL_CONTEXT_FACTORY, classname);

        String url = conn.getJndiUrl();
        if (url != null && url.length() > 0)
            properties.put(InitialContext.PROVIDER_URL, url);

        try {
            _jmsContext = new InitialContext(properties);

            String qcfUrl = conn.getQueueFactoryUrl();
            String dcfUrl = conn.getDestinationFactoryUrl();

            if (qcfUrl != null && qcfUrl.length() > 0) {
                _logger.fine( "Using queue connection factory URL '" + qcfUrl + "'" );
                _jmsConnectionFactory = (QueueConnectionFactory)_jmsContext.lookup(qcfUrl);
            }

            if (dcfUrl != null && dcfUrl.length() > 0) {
                _logger.fine( "Using destination connection factory URL '" + qcfUrl + "'" );
                _jmsConnectionFactory = (ConnectionFactory)_jmsContext.lookup(dcfUrl);
            }

            if (_jmsConnectionFactory == null) {
                String msg = "No connection factory was configured for '" + _inboundRequestEndpoint.toString() + "'";
                _logger.log(Level.WARNING, msg);
                throw new LifecycleException(msg);
            }

            _jmsInboundQueue = (Queue)_jmsContext.lookup(_inboundRequestEndpoint.getDestinationName());

            if (_outboundResponseEndpoint != null) {
                _logger.fine( "Using " + _outboundResponseEndpoint.getDestinationName() + " for outbound response messages" );
                String dname = _outboundResponseEndpoint.getDestinationName();
                _jmsOutboundQueue = (Queue)_jmsContext.lookup(dname);
            }

            if (_failureEndpoint != null) {
                _logger.fine( "Using " + _outboundResponseEndpoint.getDestinationName() + " for failure messages" );
                String dname = _failureEndpoint.getDestinationName();
                _jmsFailureQueue = (Queue)_jmsContext.lookup(dname);
            }

            int max = _inboundRequestEndpoint.getMaxConcurrentRequests();
            _logger.fine( "Allowing " + max + " concurrent request messages" );
            _fifo = new FIFOSemaphore(max);

            _initialized = true;
            _logger.info( toString() + " initialized successfully" );
        } catch (NamingException e) {
            _logger.log(Level.WARNING, "Caught NamingException initializing JMS context for '" + _inboundRequestEndpoint.toString() + "'", e);
            throw new LifecycleException(e.toString(), e);
        }
    }

    /**
     * Starts the receiver.
     */
    public synchronized void start() throws LifecycleException {
        _logger.info( "Starting " + toString() + "..." );

        if (!_initialized) throw new LifecycleException("Can't start '" + _inboundRequestEndpoint.toString() + "', it has not been successfully initialized!");

        String username = _inboundRequestEndpoint.getUsername();
        String password = _inboundRequestEndpoint.getPassword();

        JmsConnection conn = getConnection();

        if (username == null || username.length() == 0) {
            username = conn.getUsername();
            password = conn.getPassword();
        }

        try {
            _jmsInboundConnection = connect(username, password);

            if (_jmsInboundConnection instanceof QueueConnection) {
                _jmsQueueConnection = (QueueConnection)_jmsInboundConnection;
                _jmsQueueSession = _jmsQueueConnection.createQueueSession(false, // TODO parameterize
                  QueueSession.CLIENT_ACKNOWLEDGE);

                QueueReceiver receiver = _jmsQueueSession.createReceiver(_jmsInboundQueue);
                receiver.setMessageListener(new JmsMessageListener(_jmsQueueSession, this));
            } else {
                _logger.severe("Only queues are currently supported!");
            }

            _jmsInboundConnection.start();

            _logger.info( toString() + " started successfully" );
        } catch (JMSException e) {
            throw new LifecycleException(e.getMessage(), e);
        }
    }

    private Connection connect(String username, String password) throws JMSException {
        Connection conn = null;

        if (_jmsConnectionFactory instanceof QueueConnectionFactory)
            conn = ((QueueConnectionFactory)_jmsConnectionFactory).createQueueConnection(username, password);

        if (conn == null && _jmsConnectionFactory != null)
            conn = _jmsConnectionFactory.createConnection(username, password);

        if (conn != null) return conn;

        String msg = "No connection factories were able to establish a connection to " + _inboundRequestEndpoint.toString();
        _logger.warning(msg);
        throw new JMSException(msg);
    }

    /**
     * Stops the receiver, e.g. temporarily.
     */
    public synchronized void stop() throws LifecycleException {
    }

    /**
     * Closes the receiver, and any resources it may have allocated.  Note that
     * a receiver that has been closed cannot be restarted.
     * <p/>
     * Nulls all references to runtime objects.
     */
    public synchronized void close() throws LifecycleException {
        _initialized = false;

        _jmsConnectionFactory = null;
    }

}
