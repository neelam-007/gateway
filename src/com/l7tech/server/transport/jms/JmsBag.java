/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.naming.Context;
import java.io.Closeable;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Holds references to a {@link javax.jms.ConnectionFactory}, {@link javax.jms.Connection}
 * and a {@link javax.jms.Session}.
 *
 * Not thread-safe!
 */
public class JmsBag implements Closeable {
    public JmsBag( Context context, ConnectionFactory factory, Connection conn, Session sess ) {
        connectionFactory = factory;
        connection = conn;
        session = sess;
        jndiContext = context;
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

    public void close() {
        try {
            if ( session != null ) {
                try {
                    session.close();
                } catch ( Exception e ) {
                    logger.log(Level.WARNING, "Exception while closing JmsBag (session)", e);
                }
            }

            if ( connection != null ) {
                try {
                     connection.stop();
                } catch ( Exception e ) {
                    logger.log(Level.WARNING, "Exception while closing JmsBag (stop connection)", e);
                }

                try {
                    connection.close();
                } catch ( Exception e ) {
                    logger.log(Level.WARNING, "Exception while closing JmsBag (connection)", e);
                }
            }

            if ( jndiContext != null ) {
                try {
                     jndiContext.close();
                } catch ( Exception e ) {
                    logger.log(Level.WARNING, "Exception while closing JmsBag (jndi)", e);
                }
            }
        } finally {
            closed = true;
        }
    }

    private void check() {
        if (closed) throw new IllegalStateException("Bag has been closed");
    }

    private static final Logger logger = Logger.getLogger(JmsBag.class.getName());
    private final ConnectionFactory connectionFactory;
    private final Connection connection;
    private final Session session;
    private final Context jndiContext;
    private volatile boolean closed = false;
}
