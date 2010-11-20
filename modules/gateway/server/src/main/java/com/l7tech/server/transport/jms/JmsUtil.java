package com.l7tech.server.transport.jms;

import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.rmi.PortableRemoteObject;
import java.net.PasswordAuthentication;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class JmsUtil {
    private static final Logger logger = Logger.getLogger(JmsUtil.class.getName());
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final boolean detectTypes = SyspropUtil.getBoolean( "com.l7tech.server.transport.jms.detectJmsTypes", true );

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
            password = connection.getPassword();
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
        if (mapper != null)
            mapper.substitutePropertyValues(props);
        Context jndiContext = null;

        final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if ( contextClassLoader != null ) {
            Thread.currentThread().setContextClassLoader( contextClassLoader );
        }
        try {
            jndiContext = new InitialContext( props );
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

            if ( username != null && password != null ) {
                if ( preferQueue && detectTypes && connFactory instanceof QueueConnectionFactory ) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection(username, password);
                } else if ( !preferQueue && detectTypes && connFactory instanceof TopicConnectionFactory ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection(username, password);
                } else {
                    conn = connFactory.createConnection( username, password );
                }
            } else {
                if ( preferQueue && detectTypes && connFactory instanceof QueueConnectionFactory ) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection();
                } else if ( !preferQueue && detectTypes && connFactory instanceof TopicConnectionFactory ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection();
                } else {
                    conn = connFactory.createConnection( username, password );
                }
            }

            if ( createSession ) {
                if ( preferQueue && detectTypes && connFactory instanceof QueueConnectionFactory ) {
                    session = ((QueueConnection)conn).createQueueSession( transactional, acknowledgeMode );
                } else if ( !preferQueue && detectTypes && connFactory instanceof TopicConnectionFactory ) {
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
        } catch ( RuntimeException rte ) {
            throw new JmsConfigException("Error connecting to JMS : " + ExceptionUtils.getMessage(rte), rte);
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
        return connect(
                endpointCfg.getConnection(),
                endpointCfg.getEndpoint().getPasswordAuthentication(),
                endpointCfg.getPropertyMapper(),
                false,
                endpointCfg.isQueue(),
                false,
                Session.CLIENT_ACKNOWLEDGE);
    }

    public static JmsBag connect( final JmsEndpointConfig endpointCfg,
                                  final boolean transactional,
                                  final int acknowledgementMode )
        throws JmsConfigException, JMSException, NamingException
    {
        return connect(
                endpointCfg.getConnection(),
                endpointCfg.getEndpoint().getPasswordAuthentication(),
                endpointCfg.getPropertyMapper(),
                true,
                endpointCfg.isQueue(),
                transactional,
                acknowledgementMode);
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


    public static JmsBag connect(Context jndiContext,
                                 Connection conn,
                                 ConnectionFactory factory,
                                 boolean preferQueue,
                                 boolean transactional,
                                 int acknowledgementMode) throws JMSException
    {
        // check to see whether we need to create a QueueSession or TopicSession
        Session session;

        if ( preferQueue && detectTypes && factory instanceof QueueConnectionFactory ) {
            session = ((QueueConnection)conn).createQueueSession(transactional, acknowledgementMode);

        } else if ( !preferQueue && detectTypes && factory instanceof TopicConnectionFactory ) {
            session = ((TopicConnection)conn).createTopicSession(transactional, acknowledgementMode);

        } else {
            session = conn.createSession(transactional, acknowledgementMode);
        }

        return new JmsBag(jndiContext, factory, conn, session);
    }

    public static MessageConsumer createMessageConsumer( final Session session,
                                                         final Destination destination ) throws JMSException {
        MessageConsumer consumer;

        if ( detectTypes && session instanceof QueueSession && destination instanceof Queue ) {
            consumer = ((QueueSession)session).createReceiver( (Queue)destination );
        } else if ( detectTypes && session instanceof TopicSession && destination instanceof Topic ) {
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

        if ( detectTypes && session instanceof QueueSession && destination instanceof Queue ) {
            consumer = ((QueueSession)session).createReceiver( (Queue)destination, selector );
        } else if ( detectTypes && session instanceof TopicSession && destination instanceof Topic ) {
            consumer = ((TopicSession)session).createSubscriber( (Topic)destination, selector, false );
        } else {
            consumer = session.createConsumer( destination, selector );
        }

        return consumer;
    }

    public static MessageProducer createMessageProducer( final Session session,
                                                         final Destination destination ) throws JMSException {
        MessageProducer producer;

        if ( detectTypes && session instanceof QueueSession && destination instanceof Queue ) {
            // the reason for this distinction is that IBM throws java.lang.AbstractMethodError:
            // com.ibm.mq.jms.MQQueueSession.createProducer(Ljavax/jms/Destination;)Ljavax/jms/MessageProducer;
            producer = ((QueueSession)session).createSender( (Queue)destination );
        } else if ( detectTypes && session instanceof TopicSession && destination instanceof Topic ) {
            producer = ((TopicSession)session).createPublisher( (Topic)destination );
        } else {
            producer = session.createProducer( destination );
        }

        return producer;
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
}
