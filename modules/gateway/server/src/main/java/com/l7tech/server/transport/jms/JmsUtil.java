/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.gateway.common.transport.jms.JmsConnection;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import java.net.PasswordAuthentication;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsUtil {
    private static final Logger logger = Logger.getLogger(JmsUtil.class.getName());
    public static final String DEFAULT_ENCODING = "UTF-8";

    private static ClassLoader contextClassLoader;

    public static void setContextClassLoader( final ClassLoader contextClassLoader ) {
        if ( JmsUtil.contextClassLoader == null ) {
            JmsUtil.contextClassLoader = contextClassLoader;
        }
    }

    /**
     * Establishes a connection to a JMS provider, returning the necessary {@link ConnectionFactory},
     * {@link Connection} and {@link Session} inside a {@link JmsBag}.
     * <p/>
     * The {@link Connection} that is returned will not have been started.
     * <p/>
     * The JmsBag should eventually be closed by the caller, since the {@link Connection} and {@link Session}
     * objects inside are often pretty heavyweight.
     *
     * @param connection a {@link com.l7tech.gateway.common.transport.jms.JmsConnection} that encapsulates the information required
     * to connect to a JMS provider.
     * @param auth overrides the username and password from the connection if present.  May be null.
     * @param mapper property mapper for initial context properties. May be null.
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
        Session sess = null;

        Properties props = new Properties();
        props.put( Context.PROVIDER_URL, url );
        props.put( Context.INITIAL_CONTEXT_FACTORY, icf );
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

            if (!(o instanceof ConnectionFactory)) {
                String msg = "The ConnectionFactory lookup returned an unsupported object type '"
                             + o.getClass().getName() + "'.";
                logger.warning( msg );
                throw new JmsConfigException(msg);
            }

            connFactory = (ConnectionFactory) o;

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
                    throw new JmsConfigException("Could not configure connection factory.", cnfe);
                }
                catch (InstantiationException ie) {
                    throw new JmsConfigException("Could not configure connection factory.", ie);
                }
                catch (IllegalAccessException iae) {
                    throw new JmsConfigException("Could not configure connection factory.", iae);
                }
            }

            if ( username != null && password != null ) {
                if ( connFactory instanceof QueueConnectionFactory ) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection(username, password);
                    sess = ((QueueConnection)conn).createQueueSession( transactional, acknowledgeMode );
                } else if ( connFactory instanceof TopicConnectionFactory ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection(username, password);
                    sess = ((TopicConnection)conn).createTopicSession( transactional, acknowledgeMode );
                } else {
                    conn = connFactory.createConnection( username, password );
                    sess = conn.createSession( transactional, acknowledgeMode );
                }
            } else {
                if ( connFactory instanceof QueueConnectionFactory ) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection();
                    sess = ((QueueConnection)conn).createQueueSession( transactional, acknowledgeMode );
                } else if ( connFactory instanceof TopicConnectionFactory ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection();
                    sess = ((TopicConnection)conn).createTopicSession( transactional, acknowledgeMode );
                } else {
                    conn = connFactory.createConnection( username, password );
                    sess = conn.createSession( transactional, acknowledgeMode );
                }
            }

            // Give ownership of our successfully-created objects to a new JmsBag
            JmsBag result = new JmsBag( jndiContext, connFactory, conn, sess );
            conn = null;
            sess = null;
            jndiContext = null;

            logger.fine( "Connected to " + connection.toString() );

            return result;
        } catch ( RuntimeException rte ) {
            logger.log( Level.WARNING, "Caught RuntimeException while attempting to connect to JMS provider" );
            throw (JmsConfigException)new JmsConfigException(rte.toString()).initCause(rte);
        } finally {
            Thread.currentThread().setContextClassLoader(contextLoader);            
            try { if ( sess != null ) sess.close(); } catch (Throwable t) { logit(t); }
            try { if ( conn != null ) conn.close(); } catch (Throwable t) { logit(t); }
            try { if ( jndiContext != null ) jndiContext.close(); } catch (Throwable t) { logit(t); }
        }
    }


    public static JmsBag connect(final JmsEndpointConfig endpointCfg,
                                 final boolean transactional,
                                 final int acknowledgementMode)
        throws JmsConfigException, JMSException, NamingException
    {
        return connect(endpointCfg.getConnection(),
                endpointCfg.getEndpoint().getPasswordAuthentication(),
                endpointCfg.getPropertyMapper(),
                transactional, acknowledgementMode);
    }

    private static void logit( Throwable t ) {
        logger.log( Level.WARNING, "Exception during cleanup", t);
    }

    /**
     * Equivalent to {@link JmsUtil#connect(JmsConnection,java.net.PasswordAuthentication, JmsPropertyMapper,boolean,int) JmsUtil#connect(JmsConnection, null, null, false, Session.AUTO_ACKNOWLEDGE)}
     */
    public static JmsBag connect(JmsConnection connection) throws JMSException, NamingException, JmsConfigException {
        return connect(connection, null, null, false, Session.AUTO_ACKNOWLEDGE);
    }


    public static JmsBag connect(Context jndiContext,
                                 Connection conn,
                                 ConnectionFactory factory,
                                 boolean transactional, int acknowledgementMode) throws JMSException
    {
        // check to see whether we need to create a QueueSession or TopicSession
        Session sess;
        if (factory instanceof QueueConnectionFactory) {
            sess = ((QueueConnection)conn).createQueueSession(transactional, acknowledgementMode);

        } else if (factory instanceof TopicConnectionFactory) {
            sess = ((TopicConnection)conn).createTopicSession(transactional, acknowledgementMode);

        } else {
            sess = conn.createSession(transactional, acknowledgementMode);
        }

        return new JmsBag(jndiContext, factory, conn, sess);
    }


    static void closeQuietly(MessageConsumer consumer) {
        if (consumer == null) return;
        try {
            consumer.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't close Message Consumer", e);
        }
    }

    static void closeQuietly(MessageProducer mp) {
        if (mp == null) return;
        try {
            mp.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't close Message Producer", e);
        }
    }
}
