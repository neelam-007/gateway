package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.policy.variable.GatewaySecurePasswordReferenceExpander;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;

import javax.jms.*;
import javax.naming.*;
import javax.rmi.PortableRemoteObject;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.policy.variable.ServerVariables.expandSinglePasswordOnlyVariable;

/**
 * @author alex
 */
public class JmsUtil {
    private static final Logger logger = Logger.getLogger(JmsUtil.class.getName());
    public static final String DEFAULT_ENCODING = "UTF-8";
    private static final int MAX_CAUSE_DEPTH = 25;
    public static final String JMS_DESTINATION = "JMSDestination";
    public static final String JMS_DELIVERY_MODE = "JMSDeliveryMode";
    public static final String JMS_EXPIRATION = "JMSExpiration";
    public static final String JMS_PRIORITY = "JMSPriority";
    public static final String JMS_MESSAGE_ID = "JMSMessageID";
    public static final String JMS_TIMESTAMP = "JMSTimestamp";
    public static final String JMS_CORRELATION_ID = "JMSCorrelationID";
    public static final String JMS_REPLY_TO = "JMSReplyTo";
    public static final String JMS_TYPE = "JMSType";
    public static final String JMS_REDELIVERED = "JMSRedelivered";
    public static final String DELIVERY_MODE_PERSISTENT = "persistent";
    public static final String DELIVERY_MODE_NON_PERSISTENT = "nonpersistent";
    public static final boolean detectTypes = ConfigFactory.getBooleanProperty( "com.l7tech.server.transport.jms.detectJmsTypes", true );
    public static final boolean useTopicTypes = ConfigFactory.getBooleanProperty( "com.l7tech.server.transport.jms.useTopicTypes", false );

    private static enum TYPE {QUEUE, TOPIC, UNKNOWN};

    private static ClassLoader contextClassLoader;

    public static void setContextClassLoader( final ClassLoader contextClassLoader ) {
        if ( JmsUtil.contextClassLoader == null ) {
            JmsUtil.contextClassLoader = contextClassLoader;
        }
    }

    /**
     * Establishes a connection to a JMS provider, returning the necessary {@link ConnectionFactory},
     * {@link Connection} and {@link Session} (if requested) inside a {@link JmsBag}.
     * <p/>
     * The {@link Connection} that is returned will not have been started.
     * <p/>
     * The JmsBag should eventually be closed by the caller, since the {@link Connection} and {@link Session}
     * objects inside are often pretty heavyweight.
     * <p/>
     * NOTE: If createSession is false then the returned JmsBag will contain a
     * null JMS Session.
     *
     * @param connection a {@link com.l7tech.gateway.common.transport.jms.JmsConnection} that encapsulates the information required
     * to connect to a JMS provider.
     * @param auth overrides the username and password from the connection if present.  May be null.
     * @param mapper property mapper for initial context properties. May be null.
     * @param createSession true to create a session, false to skip session creation.
     * @param preferQueue true if a queue connection/session is preferred
     * @param transactional True to create a transactional session
     * @param acknowledgeMode The session acknowledgement mode (Session.AUTO_ACKNOWLEDGE) or 0 if transactional
     * @return a {@link JmsBag} containing the resulting {@link ConnectionFactory}, {@link Connection} and {@link Session}.
     * @throws JMSException
     * @throws NamingException
     * @throws JmsConfigException if no connection factory URL could be found for this connection
     */
    public static JmsBag connect(final JmsConnection connection,
                                 final PasswordAuthentication auth,
                                 final JmsPropertyMapper mapper,
                                 final boolean createSession,
                                 final boolean preferQueue,
                                 final boolean transactional,
                                 final int acknowledgeMode)
            throws JmsConfigException, JMSException, NamingException
    {
        logger.fine( "Connecting to " + connection.toString() );
        final String icf = connection.getInitialContextFactoryClassname();
        final String url = connection.getJndiUrl();
        final String dcfUrl = connection.getDestinationFactoryUrl();
        final String qcfUrl = connection.getQueueFactoryUrl();
        final String tcfUrl = connection.getTopicFactoryUrl();

        String username = null;
        String password = null;

        if ( auth != null ) {
            username = auth.getUserName();
            char[] pass = auth.getPassword();
            password = pass == null ? null : new String( pass );
        }

        if ( username == null || password == null ) {
            username = connection.getUsername();
            password = expandPassword( connection.getPassword() );
        }

        username = "\"\"".equals(username) ? "" : username;
        password = "\"\"".equals(password) ? "" : password;

        ConnectionFactory connFactory;
        Connection conn = null;
        Session session = null;

        Properties props = new Properties();
        props.setProperty( Context.PROVIDER_URL, url );
        props.setProperty( Context.INITIAL_CONTEXT_FACTORY, icf );
        props.putAll( connection.properties() );
        if ( props.getProperty( Context.SECURITY_CREDENTIALS ) != null ) {
            props.put( Context.SECURITY_CREDENTIALS, expandPassword( props.getProperty( Context.SECURITY_CREDENTIALS ) ) );
        }
        if (mapper != null)
            mapper.substitutePropertyValues(props);
        Context jndiContext = null;

        final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if ( contextClassLoader != null ) {
            Thread.currentThread().setContextClassLoader( contextClassLoader );
        }
        try {
            jndiContext = new JmsServiceLocator( props );
            String cfUrl = dcfUrl;
            if ( cfUrl == null ) cfUrl = qcfUrl;
            if ( cfUrl == null ) cfUrl = tcfUrl;

            if ( cfUrl == null ) {
                String msg = "The specified connection did not include at least one connection factory URL";
                logger.warning( msg );
                throw new JmsConfigException( msg );
            }

            logger.fine("Looking up " + cfUrl);
            Object o = jndiContext.lookup( cfUrl );
            if ( o instanceof Reference ) {
                String msg = "The ConnectionFactory lookup returned a reference to the class\n"
                             + ((Reference)o).getClassName() + ",  which cannot be loaded on the Gateway.\n"
                             + "Most likely the Gateway has not yet been configured for this JMS provider.";
                logger.warning( msg );
                throw new JmsConfigException(msg);
            }

            try {
                connFactory = cast( o, ConnectionFactory.class );
            } catch ( JMSException e ) {
                String msg = "The ConnectionFactory lookup returned an unsupported object type '"
                             + o.getClass().getName() + "'.";
                logger.warning( msg );
                throw new JmsConfigException(msg);
            }

            //noinspection SuspiciousMethodCalls
            String customizerClassname = (String) jndiContext.getEnvironment().get(JmsConnection.PROP_CUSTOMIZER);
            if (customizerClassname != null) {
                try {
                    Class customizerClass = Class.forName(customizerClassname);
                    Object instance = customizerClass.newInstance();
                    if (instance instanceof ConnectionFactoryCustomizer) {
                        ConnectionFactoryCustomizer customizer = (ConnectionFactoryCustomizer) instance;
                        customizer.configureConnectionFactory(connection, connFactory, jndiContext);
                    }
                    else {
                        throw new JmsConfigException("Could not configure connection factory, customizer does not implement the correct interface.");
                    }
                }
                catch (ClassNotFoundException cnfe) {
                    throw new JmsConfigException("Could not configure connection factory : " + ExceptionUtils.getMessage( cnfe ), cnfe);
                }
                catch (InstantiationException ie) {
                    throw new JmsConfigException("Could not configure connection factory : " + ExceptionUtils.getMessage( ie ), ie);
                }
                catch (IllegalAccessException iae) {
                    throw new JmsConfigException("Could not configure connection factory : " + ExceptionUtils.getMessage( iae ), iae);
                }
            }

            TYPE type = getType(connFactory);
            if ( username != null && password != null ) {
                if ( preferQueue && detectTypes && type == TYPE.QUEUE) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection(username, password);
                } else if ( !preferQueue && detectTypes && useTopicTypes && type == TYPE.TOPIC ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection(username, password);
                } else {
                    conn = connFactory.createConnection( username, password );
                }
            } else {
                if ( preferQueue && detectTypes && type == TYPE.QUEUE ) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection();
                } else if ( !preferQueue && detectTypes && useTopicTypes && type == TYPE.TOPIC ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection();
                } else {
                    conn = connFactory.createConnection( username, password );
                }
            }

            if ( createSession ) {
                if ( preferQueue && detectTypes && type == TYPE.QUEUE ) {
                    session = ((QueueConnection)conn).createQueueSession( transactional, acknowledgeMode );
                } else if ( !preferQueue && detectTypes && useTopicTypes && type == TYPE.TOPIC ) {
                    session = ((TopicConnection)conn).createTopicSession( transactional, acknowledgeMode );
                } else {
                    session = conn.createSession( transactional, acknowledgeMode );
                }
            }

            // Give ownership of our successfully-created objects to a new JmsBag
            JmsBag result = new JmsBag( jndiContext, connFactory, conn, session );
            conn = null;
            session = null;
            jndiContext = null;

            logger.fine( "Connected to " + connection.toString() );

            return result;
        } catch ( NoInitialContextException e ) {
            throw new JmsConfigException("Error connecting to JMS, could not create initial context : " + ExceptionUtils.getMessage(e), e);
        } catch ( RuntimeException rte ) {
            logger.log( Level.WARNING, "Caught RuntimeException while attempting to connect to JMS provider", ExceptionUtils.getDebugException(rte) );
            throw (JmsConfigException)new JmsConfigException(rte.toString()).initCause(rte);
        } finally {
            Thread.currentThread().setContextClassLoader(contextLoader);
            try { if ( session != null ) session.close(); } catch (Throwable t) { logit(t); }
            try { if ( conn != null ) conn.close(); } catch (Throwable t) { logit(t); }
            try { if ( jndiContext != null ) jndiContext.close(); } catch (Throwable t) { logit(t); }
        }
    }

    /**
     * Connect to the given endpoint but do not create a JMS session.
     *
     * <p>NOTE: The JMS Session will be null in the returned JmsBag.</p>
     *
     * @param endpointCfg The endpoint configuration to use.
     * @return The JMS Bag which will not have a JMS session
     * @throws JmsConfigException If the configuration is invalid
     * @throws JMSException If an error occurs
     * @throws NamingException If a JNDI error occurs
     */
    public static JmsBag connect( final JmsEndpointConfig endpointCfg )
        throws JmsConfigException, JMSException, NamingException
    {
        try {
            return connect(
                    endpointCfg.getConnection(),
                    endpointCfg.getEndpoint().getPasswordAuthentication(new GatewaySecurePasswordReferenceExpander(new LoggingAudit(logger))),
                    endpointCfg.getPropertyMapper(),
                    false,
                    endpointCfg.isQueue(),
                    false,
                    Session.CLIENT_ACKNOWLEDGE);
        } catch (FindException e) {
            throw new JmsConfigException("Unable to retrieve JMS endpoint password: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public static JmsBag connect( final JmsEndpointConfig endpointCfg,
                                  final boolean transactional,
                                  final int acknowledgementMode )
        throws JmsConfigException, JMSException, NamingException
    {
        try {
            return connect(
                    endpointCfg.getConnection(),
                    endpointCfg.getEndpoint().getPasswordAuthentication(new GatewaySecurePasswordReferenceExpander(new LoggingAudit(logger))),
                    endpointCfg.getPropertyMapper(),
                    true,
                    endpointCfg.isQueue(),
                    transactional,
                    acknowledgementMode);
        } catch (FindException e) {
            throw new JmsConfigException("Unable to retrieve JMS endpoint password: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static void logit( Throwable t ) {
        logger.log( Level.WARNING, "Exception during cleanup", t);
    }

    /**
     *
     */
    public static JmsBag connect(JmsConnection connection) throws JMSException, NamingException, JmsConfigException {
        return connect(connection, null, null, true, true, false, Session.AUTO_ACKNOWLEDGE);
    }

    public static MessageConsumer createMessageConsumer( final Session session,
                                                         final Destination destination ) throws JMSException {
        MessageConsumer consumer;

        TYPE type = getType(session, destination);

        if ( detectTypes && type == TYPE.QUEUE ) {
            consumer = ((QueueSession)session).createReceiver( (Queue)destination );
        } else if ( detectTypes && useTopicTypes && type == TYPE.TOPIC ) {
            consumer = ((TopicSession)session).createSubscriber( (Topic)destination );
        } else {
            consumer = session.createConsumer( destination );
        }

        return consumer;
    }

    public static MessageConsumer createMessageConsumer( final Session session,
                                                         final Destination destination,
                                                         final String selector ) throws JMSException {
        MessageConsumer consumer;
        TYPE type = getType(session, destination);

        if ( detectTypes && type == TYPE.QUEUE ) {
            consumer = ((QueueSession)session).createReceiver( (Queue)destination, selector );
        } else if ( detectTypes && useTopicTypes && type == TYPE.TOPIC ) {
            consumer = ((TopicSession)session).createSubscriber( (Topic)destination, selector, false );
        } else {
            consumer = session.createConsumer( destination, selector );
        }

        return consumer;
    }

    public static MessageProducer createMessageProducer( final Session session,
                                                         final Destination destination ) throws JMSException {
        MessageProducer producer;
        TYPE type = getType(session, destination);

        if ( detectTypes && type == TYPE.QUEUE ) {
            // the reason for this distinction is that IBM throws java.lang.AbstractMethodError:
            // com.ibm.mq.jms.MQQueueSession.createProducer(Ljavax/jms/Destination;)Ljavax/jms/MessageProducer;
            producer = ((QueueSession)session).createSender( (Queue)destination );
        } else if ( detectTypes && useTopicTypes && type == TYPE.TOPIC ) {
            producer = ((TopicSession)session).createPublisher( (Topic)destination );
        } else {
            producer = session.createProducer( destination );
        }

        return producer;
    }

    /**
     * Retrieve the destination type, the destination type can be Queue, Topic or Unknown.
     * If we cannot determine the destination type, return as Unknown
     *
     * @param session The JMS session
     * @param destination The JMS Destination
     * @return The destination type
     */
    private static TYPE getType(final Session session, final Destination destination) {
        TYPE type = TYPE.UNKNOWN;
        if (session instanceof QueueSession && destination instanceof Queue) {
            type = TYPE.QUEUE;
        }
        if (session instanceof TopicSession && destination instanceof Topic) {
            if (type == TYPE.UNKNOWN) {
                type = TYPE.TOPIC;
            } else {
                type = TYPE.UNKNOWN;
            }
        }
        return type;
    }

    /**
     * Retrieve the Connection Factory type, the Connection Factory type can be Queue, Topic or Unknown.
     * If we cannot determine the destination type, return as Unknown
     *
     * @param connectionFactory The JMS connection factory
     * @return  The Connection Factory Type
     */
    private static TYPE getType(final ConnectionFactory connectionFactory) {
        TYPE type = TYPE.UNKNOWN;
        if (connectionFactory instanceof QueueConnectionFactory) {
            type = TYPE.QUEUE;
        }
        if (connectionFactory instanceof TopicConnectionFactory) {
            if (type == TYPE.UNKNOWN) {
                type = TYPE.TOPIC;
            } else {
                type = TYPE.UNKNOWN;
            }
        }
        return type;
    }


    /**
     * Cast the given (possibly remote) object to the given class.
     *
     * @param o The object to cast (may be null)
     * @param targetClass The cast target (required)
     * @return The object cast to the target or null if o is null.
     * @throws JMSException If an error occurs casting.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T cast( final Object o, final Class<T> targetClass ) throws JMSException {
        try {
            return (T) PortableRemoteObject.narrow( o, targetClass );
        } catch ( ClassCastException cce ) {
            throw (JMSException) new JMSException( "Unable to cast object to target " + targetClass.getName() ).initCause( cce );
        }
    }

    /**
     * Get the cause of an exception.
     *
     * <p>This has special support for JMS Exceptions linked exception.</p>
     *
     * @return The cause or null.
     */
    public static Throwable getCause( final Throwable throwable ) {
        Throwable cause = null;

        if ( throwable instanceof JMSException ) {
            cause = ((JMSException) throwable).getLinkedException();
        }

        if ( cause == null ) {
            cause = throwable.getCause();
        }

        return cause;
    }

    /**
     * Is the given exception caused by an "expected" JMS exception.
     *
     * <p>An expected exception is one with a well known cause for which
     * additional information (such as a stack trace) is not useful.</p>
     *
     * @param throwable The throwable to test
     * @return true if the throwable or a cause is an expected JMS exception.
     */
    public static boolean isCausedByExpectedJMSException( final Throwable throwable ) {
        boolean expected = false;

        int count = 0;
        Throwable cause = throwable;
        while ( cause != null && count++ < MAX_CAUSE_DEPTH ) {
            if ( cause instanceof InvalidClientIDException ||
                 cause instanceof InvalidDestinationException ||
                 cause instanceof JMSSecurityException ||
                 cause instanceof ResourceAllocationException ) {
                expected = true;
                break;
            }

            final boolean isJmsException = cause instanceof JMSException;
            cause = getCause( cause );

            if ( isJmsException &&
                 ( cause instanceof UnknownHostException ||
                   cause instanceof SocketException ||
                   cause instanceof SocketTimeoutException ) )  {
                expected = true;
            }
        }

        return expected;
    }

    /**
     * Is the given exception caused by an "expected" JNDI exception.
     *
     * <p>An expected exception is one with a well known cause for which
     * additional information (such as a stack trace) is not useful.</p>
     *
     * @param throwable The throwable to test
     * @return true if the throwable or a cause is an expected JNDI exception.
     */
    public static boolean isCausedByExpectedNamingException( final Throwable throwable ) {
        boolean expected = false;

        int count = 0;
        Throwable cause = throwable;
        while ( cause != null && count++ < MAX_CAUSE_DEPTH ) {
            if ( cause instanceof CommunicationException ||
                 cause instanceof InsufficientResourcesException ||
                 cause instanceof InvalidNameException ||
                 cause instanceof LimitExceededException ||
                 cause instanceof NameNotFoundException ||
                 cause instanceof NamingSecurityException ||
                 cause instanceof NoInitialContextException ||
                 cause instanceof ServiceUnavailableException ) {
                expected = true;
                break;
            }

            cause = getCause( cause );
        }

        return expected;
    }

    /**
     * Get the error message for the given JMS exception.
     *
     * @param exception The JMS Exception
     * @return The error message including error code and any cause(s)
     */
    public static String getJMSErrorMessage( final JMSException exception ) {
        final StringBuilder builder = new StringBuilder();

        if ( exception instanceof InvalidClientIDException ) {
            builder.append( "Invalid client identifier; " );
        } else if ( exception instanceof InvalidDestinationException ) {
            builder.append( "Invalid destination; " );
        } else if ( exception instanceof JMSSecurityException ) {
            builder.append( "Security error; " );
        } else if ( exception instanceof ResourceAllocationException ) {
            builder.append( "Resource allocation error; " );
        }

        builder.append( ExceptionUtils.getMessage( exception ) );

        if ( exception.getErrorCode() != null ) {
            builder.append( ", error code: " );
            builder.append( exception.getErrorCode() );
        }

        appendCauses( exception, builder );

        return builder.toString();
    }

    /**
     * Get an error message for the given naming exception.
     *
     * @param exception The throwable
     * @return The error message including error code and any cause(s)
     */
    public static String getJNDIErrorMessage( final NamingException exception ) {
        final StringBuilder builder = new StringBuilder();

        if ( exception instanceof CommunicationException ) {
            builder.append( "Communication error; " );
        } else if ( exception instanceof InsufficientResourcesException ) {
            builder.append( "Insufficient resources; " );
        } else if ( exception instanceof InvalidNameException ) {
            builder.append( "Invalid name; " );
        } else if ( exception instanceof LimitExceededException ) {
            builder.append( "Limit exceeded; " );
        } else if ( exception instanceof NameNotFoundException ) {
            builder.append( "Name not found; " );
        }else if ( exception instanceof NamingSecurityException ) {
            builder.append( "Security error; " );
        }else if ( exception instanceof NoInitialContextException ) {
            builder.append( "No initial context; " );
        }else if ( exception instanceof ServiceUnavailableException ) {
            builder.append( "Service unavailable; " );
        }

        builder.append( ExceptionUtils.getMessage( exception ) );

        if ( exception.getResolvedName() != null ) {
            builder.append( ", resolved name: " );
            builder.append( exception.getResolvedName() );
        }

        if ( exception.getRemainingName() != null ) {
            builder.append( ", remaining name: " );
            builder.append( exception.getRemainingName() );
        }

        appendCauses( exception, builder );

        return builder.toString();
    }

    /**
     * Retrieves all JMS headers from the given Message and stores them in a map.
     * <p/>
     * Possible headers:
     * <p/>
     * JMSDestination<br />
     * JMSDeliveryMode<br />
     * JMSExpiration<br />
     * JMSPriority<br />
     * JMSMessageID<br />
     * JMSTimestamp<br />
     * JMSCorrelationID<br />
     * JMSReplyTo<br />
     * JMSType<br />
     * JMSRedelivered<br />
     *
     * @param jmsMessage the JMS message from which to retrieve headers.
     * @return a map of JMS headers present on the given message.
     * @throws JMSException if an error occurs retrieving headers from the given message.
     */
    public static Map<String, String> getJmsHeaders(final Message jmsMessage) throws JMSException {
        final Map<String, String> headers = new HashMap<String, String>();
        if (jmsMessage.getJMSDestination() != null) {
            headers.put(JMS_DESTINATION, jmsMessage.getJMSDestination().toString());
        }
        final int deliveryMode = jmsMessage.getJMSDeliveryMode();
        if(deliveryMode == DeliveryMode.NON_PERSISTENT){
            headers.put(JMS_DELIVERY_MODE, DELIVERY_MODE_NON_PERSISTENT);
        }else if(deliveryMode == DeliveryMode.PERSISTENT){
            headers.put(JMS_DELIVERY_MODE, DELIVERY_MODE_PERSISTENT);
        }
        headers.put(JMS_EXPIRATION, String.valueOf(jmsMessage.getJMSExpiration()));
        headers.put(JMS_PRIORITY, String.valueOf(jmsMessage.getJMSPriority()));
        if (jmsMessage.getJMSMessageID() != null) {
            headers.put(JMS_MESSAGE_ID, jmsMessage.getJMSMessageID());
        }
        headers.put(JMS_TIMESTAMP, String.valueOf(jmsMessage.getJMSTimestamp()));
        if (jmsMessage.getJMSCorrelationID() != null) {
            headers.put(JMS_CORRELATION_ID, jmsMessage.getJMSCorrelationID());
        }
        if (jmsMessage.getJMSReplyTo() != null) {
            headers.put(JMS_REPLY_TO, jmsMessage.getJMSReplyTo().toString());
        }
        if (jmsMessage.getJMSType() != null) {
            headers.put(JMS_TYPE, jmsMessage.getJMSType());
        }
        headers.put(JMS_REDELIVERED, String.valueOf(jmsMessage.getJMSRedelivered()));
        return headers;
    }

    private static String expandPassword( final String passwordExpression ) throws JmsConfigException {
        try {
            return passwordExpression == null ?
                    null :
                    expandSinglePasswordOnlyVariable( new LoggingAudit( logger ), passwordExpression );
        } catch ( FindException e ) {
            throw new JmsConfigException("Unable to retrieve JMS password: " + ExceptionUtils.getMessage( e ), e);
        }
    }

    private static void appendCauses( final Exception exception, final StringBuilder builder ) {
        int count = 0;
        Throwable cause = getCause( exception );
        while ( cause != null && count++ < MAX_CAUSE_DEPTH ) {
            builder.append( ", caused by " );
            builder.append( ExceptionUtils.getMessage( cause ) );
            cause = getCause( cause );
        }
    }
}
