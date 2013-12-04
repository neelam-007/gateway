package com.l7tech.server.transport.jms;

import com.l7tech.util.ExceptionUtils;

import javax.jms.*;
import javax.naming.Context;
import java.io.Closeable;
import java.lang.IllegalStateException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds references to a {@link javax.jms.ConnectionFactory}, {@link javax.jms.Connection}
 * and a {@link javax.jms.Session}.
 *
 * Not thread-safe!
 */
public class JmsBag implements Closeable {
    public JmsBag( Context context, ConnectionFactory factory, Connection conn, Session sess, MessageConsumer consumer, MessageProducer producer, Object owner) {
        connectionFactory = factory;
        connection = conn;
        session = sess;
        jndiContext = context;
        messageConsumer = consumer;
        messageProducer = producer;
        bagOwner = owner;
    }

    public JmsBag( Context context, ConnectionFactory factory, Connection conn, Session sess, Object owner ) {
        this(context, factory, conn, sess, null, null, owner);
    }


    public ConnectionFactory getConnectionFactory() {
        check();
        return connectionFactory;
    }

    public Connection getConnection() {
        check();
        return connection;
    }

    public Session getSession() {
        check();
        return session;
    }

    public Context getJndiContext() {
        check();
        return jndiContext;
    }

    public Object getBagOwner() {
        return bagOwner;
    }

    public MessageConsumer getMessageConsumer() {
        return messageConsumer;
    }

    /**
     * Close the session and the consumer only.
     */
    public void closeSession() {
        if (messageConsumer != null) {
            try {
                messageConsumer.close();
            } catch (Exception e) {
                handleCloseError( "consumer", e );
            }
            messageConsumer = null;
        }

        if (messageProducer != null) {
            try {
                messageProducer.close();
            } catch (Exception e) {
                handleCloseError( "failureQueueProducer", e );
            }
            messageProducer = null;
        }


        if ( session != null ) {
            try {
                session.close();
            } catch ( Exception e ) {
                handleCloseError( "session", e );
            }
            session = null;
        }
    }

    @Override
    public void close() {
        if ( !closed ) {
            try {
                closeSession();

                if ( connection != null ) {
                    try {
                         connection.stop();
                    } catch ( Exception e ) {
                        handleCloseError( "stop connection", e );
                    }

                    try {
                        connection.close();
                    } catch ( Exception e ) {
                        handleCloseError( "connection", e );
                    }
                }

                if ( jndiContext != null ) {
                    try {
                         jndiContext.close();
                    } catch ( Exception e ) {
                        handleCloseError( "jndi", e );
                    }
                }
            } finally {
                closed = true;
            }
        }
    }

    private void handleCloseError( final String detail, final Exception exception ) {
        logger.log(
                Level.WARNING,
                "Error closing JmsBag ("+detail+"): " + ExceptionUtils.getMessage( exception ),
                ExceptionUtils.getDebugException(exception) );
    }

    private void check() {
        if (closed) throw new IllegalStateException("Bag has been closed");
    }

    public MessageProducer getMessageProducer() {
        return messageProducer;
    }

    private static final Logger logger = Logger.getLogger(JmsBag.class.getName());
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Context jndiContext;
    private MessageConsumer messageConsumer;
    //This can be failure queue producer for inbound or producer for outbound
    private MessageProducer messageProducer;
    private Object bagOwner;
    protected volatile boolean closed;

}
