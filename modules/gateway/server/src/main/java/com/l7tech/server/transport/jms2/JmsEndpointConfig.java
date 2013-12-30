package com.l7tech.server.transport.jms2;

import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.JmsDynamicProperties;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsPropertyMapper;
import com.l7tech.server.transport.jms.JmsUtil;
import org.springframework.context.ApplicationContext;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Properties;

/**
 * JmsEndpointConfig encapsulates all information necessary to use a JMS destination.
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
    /** Flag specifying whether the endpoint is dynamic */
    private Boolean dynamic;
    /** Spring application context */
    private final ApplicationContext appContext;
    /** String denoting the display name for an endpoint instance */
    private final String displayName;
    /** String identifier representing this JMS endpoint configuration  */
    private JmsEndpointKey endpointKey;

    private boolean evictOnExpired = true;

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
        this.dynamic = endpoint.isTemplate() || connection.isTemplate();
        if (dynamic) {
            // clone the JmsConnection and JmsEndpoint beans since they will be overwritten
            final JmsConnection connCopy = new JmsConnection(connection, false);
            final JmsEndpoint endpointCopy = new JmsEndpoint(endpoint, false);

            applyOverrides(connCopy, endpointCopy, dynamicProperties);

            this.conn = new JmsConnection(connCopy, true);
            this.endpoint = new JmsEndpoint(endpointCopy, true);
        } else {
            this.conn = new JmsConnection(connection, true);
            this.endpoint = new JmsEndpoint(endpoint, true);
        }

        this.propertyMapper = propertyMapper;
        this.appContext = appContext;

        StringBuilder sb = new StringBuilder(getEndpoint().getName()).append(",").append(getConnection().getJndiUrl()).append("/").append(getEndpoint().getDestinationName());
        this.displayName =  sb.toString();
    }

    public boolean isEvictOnExpired() {
        return evictOnExpired;
    }

    public void setEvictOnExpired(boolean evictOnExpired) {
        this.evictOnExpired = evictOnExpired;
    }

    /* Getters */
    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    /**
     * Get the (read only) JMS connection.
     *
     * @return The JMS connection.
     */
    public JmsConnection getConnection() {
        return conn;
    }

    /**
     * Get the (read only) JMS endpoint.
     *
     * @return The JMS endpoint.
     */
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
     * @param request The message being replied to.
     * @param jndiContext The JNDI Context to use.
     * @return The JMS Destination to which responses should be sent, possibly null.
     * @throws javax.jms.JMSException
     */
    Destination getResponseDestination( final Message request, 
                                        final Context jndiContext ) throws JMSException, NamingException {
        if (JmsReplyType.NO_REPLY.equals(getReplyType())) {
            return null;
        } else if (JmsReplyType.AUTOMATIC.equals(getReplyType())) {
            return request.getJMSReplyTo();
        } else if (JmsReplyType.REPLY_TO_OTHER.equals(getReplyType())) {
            String replyToQueueName = endpoint.getReplyToQueueName();
            return JmsUtil.cast(jndiContext.lookup(replyToQueueName), Destination.class);
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
     * Returns the endpoint key for the specific JMS endpoint.
     *
     * @return The key for this endpoint configuration
     */
    public JmsEndpointKey getJmsEndpointKey() {
        if ( endpointKey == null) {
            endpointKey = new JmsEndpointKey( endpoint, conn );
        }
        return endpointKey;
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

    /**
     * Is the destination a Queue?
     *
     * @return True for a Queue, False for a Topic
     */
    public boolean isQueue() {
        return endpoint.isQueue();
    }

    /**
     * Validate this endpoint configuration.
     *
     * @throws JmsConfigException If the configuration is invalid. 
     */
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
     * Override empty template settings with values from the given dynamic properties.
     */
    private void applyOverrides( final JmsConnection jmsConnection,
                                 final JmsEndpoint jmsEndpoint,
                                 final JmsDynamicProperties overrides ) {
        //get all the empty fields and populate them from the dynamic properties
        if (jmsConnection.getInitialContextFactoryClassname() == null || "".equals(jmsConnection.getInitialContextFactoryClassname()))
            jmsConnection.setInitialContextFactoryClassname(overrides.getIcfName());

        if (jmsConnection.getJndiUrl() == null || "".equals(jmsConnection.getJndiUrl()))
            jmsConnection.setJndiUrl(overrides.getJndiUrl());

        Properties jmsConnectionProperties = jmsConnection.properties();
        boolean isModified = false;
        String jndiUserName = (String) jmsConnectionProperties.get(Context.SECURITY_PRINCIPAL);
        if (jndiUserName != null && "".equals(jndiUserName)) {
            jmsConnectionProperties.setProperty(Context.SECURITY_PRINCIPAL, overrides.getJndiUserName());
            isModified = true;
        }
        String jndiPassword = (String) jmsConnectionProperties.get(Context.SECURITY_CREDENTIALS);
        if (jndiPassword != null && "".equals(jndiPassword)) {
            jmsConnectionProperties.setProperty(Context.SECURITY_CREDENTIALS, overrides.getJndiPassword());
            isModified = true;
        }
        if (isModified) {
            jmsConnection.properties(jmsConnectionProperties);
        }

        if (jmsConnection.getQueueFactoryUrl() == null || "".equals(jmsConnection.getQueueFactoryUrl()))
            jmsConnection.setQueueFactoryUrl(overrides.getQcfName());

        if (jmsEndpoint.getReplyToQueueName() == null || "".equals(jmsEndpoint.getReplyToQueueName()))
            jmsEndpoint.setReplyToQueueName(overrides.getReplytoQName());

        if (jmsEndpoint.getDestinationName() == null || "".equals(jmsEndpoint.getDestinationName()))
            jmsEndpoint.setDestinationName(overrides.getDestQName());

        if (jmsEndpoint.getUsername() != null && "".equals(jmsEndpoint.getUsername()))
            jmsEndpoint.setUsername(overrides.getDestUserName());

        if (jmsEndpoint.getPassword() != null && "".equals(jmsEndpoint.getPassword()))
            jmsEndpoint.setPassword(overrides.getDestPassword());
    }

    public static final class JmsEndpointKey {
        private final Goid jmsEndpointGoid;
        private final int jmsEndpointVersion;
        private final Goid jmsConnectionGoid;
        private final int jmsConnectionVersion;
        private final String initialContextFactoryClassname;
        private final String jndiUrl;
        private final String queueFactoryUrl;
        private final String destinationQueue;
        private final String replyToQueue;

        private JmsEndpointKey( final JmsEndpoint endpoint,
                                final JmsConnection connection ) {
            this.jmsEndpointGoid = endpoint.getGoid();
            this.jmsEndpointVersion = endpoint.getVersion();
            this.jmsConnectionGoid = connection.getGoid();
            this.jmsConnectionVersion = connection.getVersion();

            if ( endpoint.isTemplate() || connection.isTemplate() ) {
                this.initialContextFactoryClassname = connection.getInitialContextFactoryClassname();
                this.jndiUrl = connection.getJndiUrl();
                this.queueFactoryUrl = connection.getQueueFactoryUrl();
                this.destinationQueue = endpoint.getDestinationName();
                this.replyToQueue = endpoint.getReplyToQueueName();
            } else {
                this.initialContextFactoryClassname = null;
                this.jndiUrl = null;
                this.queueFactoryUrl = null;
                this.destinationQueue = null;
                this.replyToQueue = null;
            }
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final JmsEndpointKey that = (JmsEndpointKey) o;

            if ( jmsConnectionGoid != null ? !jmsConnectionGoid.equals( that.jmsConnectionGoid ) : that.jmsConnectionGoid != null ) return false;
            if ( jmsConnectionVersion != that.jmsConnectionVersion ) return false;
            if ( jmsEndpointGoid != null ? !jmsEndpointGoid.equals( that.jmsEndpointGoid ) : that.jmsEndpointGoid != null ) return false;
            if ( jmsEndpointVersion != that.jmsEndpointVersion ) return false;
            if ( destinationQueue != null ? !destinationQueue.equals( that.destinationQueue ) : that.destinationQueue != null )
                return false;
            if ( initialContextFactoryClassname != null ? !initialContextFactoryClassname.equals( that.initialContextFactoryClassname ) : that.initialContextFactoryClassname != null )
                return false;
            if ( jndiUrl != null ? !jndiUrl.equals( that.jndiUrl ) : that.jndiUrl != null ) return false;
            if ( queueFactoryUrl != null ? !queueFactoryUrl.equals( that.queueFactoryUrl ) : that.queueFactoryUrl != null )
                return false;
            if ( replyToQueue != null ? !replyToQueue.equals( that.replyToQueue ) : that.replyToQueue != null )
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = jmsEndpointGoid.hashCode();
            result = 31 * result + jmsEndpointVersion;
            result = 31 * result + jmsConnectionGoid.hashCode();
            result = 31 * result + jmsConnectionVersion;
            result = 31 * result + (initialContextFactoryClassname != null ? initialContextFactoryClassname.hashCode() : 0);
            result = 31 * result + (jndiUrl != null ? jndiUrl.hashCode() : 0);
            result = 31 * result + (queueFactoryUrl != null ? queueFactoryUrl.hashCode() : 0);
            result = 31 * result + (destinationQueue != null ? destinationQueue.hashCode() : 0);
            result = 31 * result + (replyToQueue != null ? replyToQueue.hashCode() : 0);
            return result;
        }

        /**
         * Get a String representation of this key.
         *
         * <p>The representation should include all properties of the key.</p>
         *
         * @return The string representation.
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("JmsEndpointKey[");
            sb.append(jmsEndpointGoid);
            sb.append(',');
            sb.append(jmsEndpointVersion);
            sb.append('-');
            sb.append(jmsConnectionGoid);
            sb.append(',');
            sb.append(jmsConnectionVersion);

            /*
             * For dynamic endpoints, append the full JMS destination (JNDI, QName, QCF, etc)
             */
            if ( initialContextFactoryClassname != null ) {
                sb.append("-");
                sb.append(initialContextFactoryClassname);
                sb.append(',');
                sb.append(jndiUrl);
                sb.append(',');
                sb.append(queueFactoryUrl);
                sb.append(',');
                sb.append(destinationQueue);
                sb.append(',');
                sb.append(replyToQueue);
            }

            sb.append("]");

            return sb.toString();
        }
    }
}
