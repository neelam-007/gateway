package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.policy.JmsDynamicProperties;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsPropertyMapper;
import org.springframework.context.ApplicationContext;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * JmsEndpointConfig encapsulates all information necessary to use a JMS destination.
 *
 * @author: vchan
 */
public class JmsEndpointConfig {

    /** Separator used in the endpoint DisplayName */
    private static final String SEPARATOR = "/";

    /** Configured Jms connection attributes */
    private final JmsConnection conn;
    /** Configured Jms endpoint attributes */
    private final JmsEndpoint endpoint;
    /** Property mapper for Jms */
    private final JmsPropertyMapper propertyMapper;
    /** Flag specifying whether the endpoint should handle messages transactionally */
    private Boolean transactional;
    /** Flag specifying whether the endpoint is dynamic */
    private Boolean dynamic;
    /** Spring application context */
    private final ApplicationContext appContext;
    /** String denoting the display name for an endpoint instance */
    private final String displayName;
    /** String identifier representing this JMS endpoint configuration  */
    private String endpointIdentifier;

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
        this(connection, endpoint, propertyMapper, appContext, new JmsDynamicProperties());
    }

    /**
     * Constructor using overrides.
     *
     * @param connection configured Jms connection attributes
     * @param endpoint configured Jms endpoint attributes
     * @param propertyMapper mapper for Jms initial context properties
     * @param appContext spring application context
     * @param dynamicProperties dynamic JMS queue configuration values
     */
    public JmsEndpointConfig(final JmsConnection connection,
                             final JmsEndpoint endpoint,
                             final JmsPropertyMapper propertyMapper,
                             final ApplicationContext appContext,
                             final JmsDynamicProperties dynamicProperties)
    {
        // check if this is a template endpoint
        this.dynamic = endpoint.isTemplate();
        if (dynamic) {
            // clone the JmsConnection and JmsEndpoint beans since they will be overwritten
            JmsConnection connClone = new JmsConnection();
            connClone.copyFrom(connection);
            this.conn = connClone;

            JmsEndpoint endpointClone = new JmsEndpoint();
            endpointClone.copyFrom(endpoint);
            this.endpoint = endpointClone;

            applyOverrides(dynamicProperties);
        } else {
            this.conn = connection;
            this.endpoint = endpoint;
        }

        this.propertyMapper = propertyMapper;
        this.appContext = appContext;

        StringBuffer sb = new StringBuffer(getConnection().getJndiUrl()).append(SEPARATOR).append(getConnection().getName());
        this.displayName =  sb.toString();
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
        if (JmsReplyType.NO_REPLY.equals(getReplyType())) {
            return null;
        } else if (JmsReplyType.AUTOMATIC.equals(getReplyType())) {
            return request.getJMSReplyTo();
        } else if (JmsReplyType.REPLY_TO_OTHER.equals(getReplyType())) {
            // use bag's jndi context to lookup the the destination and create the sender
            Context jndiContext = bag.getJndiContext();
            String replyToQueueName = endpoint.getReplyToQueueName();
            return (Destination)jndiContext.lookup(replyToQueueName);
        } else {
            // This should never occur!
            String msg = "Unknown JmsReplyType " + getReplyType().toString();
            throw new javax.jms.IllegalStateException(msg);
        }
    }

    /**
     * Returns the endpoint display name.
     *
     * @return String for the endpoint display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the endpoint Id for the specific JMS endpoint.
     *
     * @return String for the endpoint display name
     */
    public String getEndpointIdentifier() {

        if (endpointIdentifier == null) {
            StringBuffer sb = new StringBuffer().append(endpoint.getOid()).append("-").append(conn.getOid());

            /*
             * For dynamic endpoints, append the full JMS destination (JNDI, QName, and QCF)
             */
            if (isDynamic()) {
                sb.append("-dest-").append(getDisplayName());
                sb.append(SEPARATOR).append(getConnection().getQueueFactoryUrl()); // append QCF
            }

            endpointIdentifier = sb.toString();
        }
        return endpointIdentifier;
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

    /**
     * Returns whether the is dynamic.
     *
     * @return true if the outbound JMS queue was configured using context variables for queue destination
     */
    public boolean isDynamic() {
        return dynamic;
    }

    public void validate() throws JmsConfigException {
        if ( conn.getInitialContextFactoryClassname() == null || "".equals(conn.getInitialContextFactoryClassname()) ) {
            throw new JmsConfigException( "Initial context factory class name is required" );
        }

        if (conn.getJndiUrl() == null || "".equals(conn.getJndiUrl())) {
            throw new JmsConfigException( "JNDI URL is required" );
        }

        if (conn.getQueueFactoryUrl() == null || "".equals(conn.getQueueFactoryUrl())) {
            throw new JmsConfigException( "Queue connection factory name is required" );
        }

        if (endpoint.getReplyType()==JmsReplyType.REPLY_TO_OTHER && (endpoint.getReplyToQueueName() == null || "".equals(endpoint.getReplyToQueueName()))) {
            throw new JmsConfigException( "Reply to queue is required" );
        }

        if (endpoint.getDestinationName() == null || "".equals(endpoint.getDestinationName())) {
            throw new JmsConfigException( "Destination queue name is required" );
        }
    }

    /**
     * Note: this could be a problem if the ctx var type is of Message rather than String
     */
    private void applyOverrides( final JmsDynamicProperties overrides ) {
        //get all the empty fields and populate them from the dynamic properties
        if (conn.getInitialContextFactoryClassname() == null || "".equals(conn.getInitialContextFactoryClassname()))
            conn.setInitialContextFactoryClassname(overrides.getIcfName());

        if (conn.getJndiUrl() == null || "".equals(conn.getJndiUrl()))
            conn.setJndiUrl(overrides.getJndiUrl());

        if (conn.getQueueFactoryUrl() == null || "".equals(conn.getQueueFactoryUrl()))
            conn.setQueueFactoryUrl(overrides.getQcfName());

        if (endpoint.getReplyToQueueName() == null || "".equals(endpoint.getReplyToQueueName()))
            endpoint.setReplyToQueueName(overrides.getReplytoQName());

        if (endpoint.getDestinationName() == null || "".equals(endpoint.getDestinationName()))
            endpoint.setDestinationName(overrides.getDestQName());
    }
}
