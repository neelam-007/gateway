/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.logging.LogManager;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.PasswordAuthentication;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsUtil {

    public static class JmsConfigException extends Exception {
        JmsConfigException( String message ) {
            super( message );
        }
    }

    /**
     * Establishes a connection to a JMS provider, returning the necessary {@link ConnectionFactory},
     * {@link Connection} and {@link Session} inside a {@link JmsBag}.
     * <p/>
     * The JmsBag should eventually be closed by the caller.
     *
     * @param connection a {@link JmsConnection} that encapsulates the information required
     * to connect to a JMS provider.
     * @param auth overrides the username and password from the connection if present.  May be null.
     * @return a {@link JmsBag} containing the resulting {@link ConnectionFactory}, {@link Connection} and {@link Session}.
     * @throws JMSException
     * @throws NamingException
     * @throws JmsConfigException if no connection factory URL could be found for this connection
     */
    public static JmsBag connect( JmsConnection connection, PasswordAuthentication auth ) throws JmsConfigException, JMSException, NamingException {
        String icf = connection.getInitialContextFactoryClassname();
        String url = connection.getJndiUrl();
        String dcfUrl = connection.getDestinationFactoryUrl();
        String qcfUrl = connection.getQueueFactoryUrl();
        String tcfUrl = connection.getTopicFactoryUrl();

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

        ConnectionFactory connFactory = null;
        Connection conn = null;
        Session sess = null;

        _logger.finer( "Getting JNDI InitialContext" );
        Properties props = new Properties();
        props.put( Context.PROVIDER_URL, url );
        props.put( Context.INITIAL_CONTEXT_FACTORY, icf );
        Context jndiContext = null;

        try {
            jndiContext = new InitialContext( props );
            String cfUrl = dcfUrl;
            if ( cfUrl == null ) cfUrl = qcfUrl;
            if ( cfUrl == null ) cfUrl = tcfUrl;

            if ( cfUrl == null ) {
                String msg = "The specified connection did not include at least one connection factory URL";
                _logger.warning( msg );
                throw new JmsConfigException( msg );
            }

            _logger.finer( "Finding JMS connection factory" );
            connFactory = (ConnectionFactory)jndiContext.lookup( cfUrl );
            if ( username != null && password != null ) {
                if ( connFactory instanceof QueueConnectionFactory ) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection(username, password);
                    sess = ((QueueConnection)conn).createQueueSession( false, Session.AUTO_ACKNOWLEDGE );
                } else if ( connFactory instanceof TopicConnectionFactory ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection(username, password);
                    sess = ((TopicConnection)conn).createTopicSession( false, Session.AUTO_ACKNOWLEDGE );
                } else {
                    conn = connFactory.createConnection( username, password );
                    sess = conn.createSession( false, Session.AUTO_ACKNOWLEDGE );
                }
            } else {
                if ( connFactory instanceof QueueConnectionFactory ) {
                    conn = ((QueueConnectionFactory)connFactory).createQueueConnection();
                    sess = ((QueueConnection)conn).createQueueSession( false, Session.AUTO_ACKNOWLEDGE );
                } else if ( connFactory instanceof TopicConnectionFactory ) {
                    conn = ((TopicConnectionFactory)connFactory).createTopicConnection();
                    sess = ((TopicConnection)conn).createTopicSession( false, Session.AUTO_ACKNOWLEDGE );
                } else {
                    conn = connFactory.createConnection( username, password );
                    sess = conn.createSession( false, Session.AUTO_ACKNOWLEDGE );
                }
            }

            // Give ownership of our successfully-created objects to a new JmsBag
            JmsBag result = new JmsBag( jndiContext, connFactory, conn, sess );
            conn = null;
            sess = null;
            jndiContext = null;

            return result;
        } finally {
            try { if ( sess != null ) sess.close(); } catch (Throwable t) { logit(t); }
            try { if ( conn != null ) conn.close(); } catch (Throwable t) { logit(t); }
            try { if ( jndiContext != null ) jndiContext.close(); } catch (Throwable t) { logit(t); }
        }
    }

    private static void logit( Throwable t ) {
        _logger.log( Level.WARNING, "Exception during cleanup", t);
    }

    /**
     * Equivalent to {@link JmsUtil#connect(JmsConnection, null)}
     */
    public static JmsBag connect( JmsConnection connection ) throws JMSException, NamingException, JmsConfigException {
        return connect( connection, null );
    }

    private static final Logger _logger = LogManager.getInstance().getSystemLogger();
}
