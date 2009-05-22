package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsPropertyMapper;
import org.springframework.context.ApplicationContext;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * POJO specifying one inbound Jms endpoint configured for the gateway.
 *
 * @author: vchan
 */
public class JmsEndpointConfig {

    /** Configured Jms connection attributes */
    private final JmsConnection conn;
    /** Configured Jms endpoint attributes */
    private final JmsEndpoint endpoint;
    /** Property mapper for Jms */
    private final JmsPropertyMapper propertyMapper;
    /** Flag specifying whether the endpoint should handle messages transactionally */
    private Boolean transactional;
    /** Spring application context */
    private final ApplicationContext appContext;
    /** String denoting the display name for an endpoint instance */
    private final String displayName;

    /**
     * Constructor.
     *
     * @param connection configured Jms connection attributes
     * @param endpoint configured Jms endpoint attributes
     * @param propertyMapper mapper for Jms initial context properties
     * @param appContext spring application context
     */
    public JmsEndpointConfig(final JmsConnection connection,
                             final JmsEndpoint endpoint,
                             final JmsPropertyMapper propertyMapper,
                             final ApplicationContext appContext)
    {
        super();
        this.conn = connection;
        this.endpoint = endpoint;
        this.propertyMapper = propertyMapper;
        this.appContext = appContext;
        this.displayName = getConnection().getJndiUrl() + "/" + getConnection().getName();
    }

    /* Getters */
    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    public JmsConnection getConnection() {
        return conn;
    }

    public JmsEndpoint getEndpoint() {
        return endpoint;
    }

    public JmsReplyType getReplyType() {
        return endpoint.getReplyType();
    }

    public JmsPropertyMapper getPropertyMapper() {
        return propertyMapper;
    }

    /**
     * Finds the proper JMS destination to send a response to.
     * <p/>
     * <b>This method can return null</b> under some circumstances:
     * <ul>
     * <li>If the ReplyType is {@link JmsReplyType#NO_REPLY};
     * <li>If the ReplyType is {@link JmsReplyType#AUTOMATIC} but the
     * incoming request specified no ReplyTo queue.
     *
     * @return The JMS Destination to which responses should be sent, possibly null.
     * @throws javax.jms.JMSException
     */
    Destination getResponseDestination(Message request, JmsBag bag) throws JMSException, NamingException {

        // TODO: Fix the #*^&$*&^@ logging
        if (JmsReplyType.NO_REPLY.equals(getReplyType())) {

//            _logger.finer("Returning NO_REPLY (null) for '" + toString() + "'");
            return null;

        } else if (JmsReplyType.AUTOMATIC.equals(getReplyType())) {

//            _logger.finer("Returning AUTOMATIC '" + request.getJMSReplyTo() +
//              "' for '" + toString() + "'");
            return request.getJMSReplyTo();

        } else if (JmsReplyType.REPLY_TO_OTHER.equals(getReplyType())) {

            // use bag's jndi context to lookup the the destination and create the sender
            Context jndiContext = bag.getJndiContext();
            String replyToQueueName = endpoint.getReplyToQueueName();
//            _logger.fine("looking up destination name " + replyToQueueName);
            return (Destination)jndiContext.lookup(replyToQueueName);

        } else {

            // This should never occur!
            String msg = "Unknown JmsReplyType " + getReplyType().toString();
//            _logger.severe(msg);
            throw new javax.jms.IllegalStateException(msg);
        }
    }

    /**
     * Returns the endpoing display name.
     *
     * @return String for the endpoint display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns whether the endpoint is configured to be transactional.
     *
     * @return true if the endpoint JmsAcknowledgementType=ON_COMPLETE, false otherwise
     */
    public boolean isTransactional() {

        if (transactional == null)
            transactional = (this.endpoint.getAcknowledgementType() == JmsAcknowledgementType.ON_COMPLETION);
        return transactional;
    }
}
