/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.logging.LogManager;

import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.logging.Logger;

/**
 * Holds references to a {@link javax.jms.ConnectionFactory}, {@link javax.jms.Connection}
 * and a {@link javax.jms.Session}.
 *
 * Not thread-safe!
 */
public class JmsBag {
    public JmsBag( Context context, ConnectionFactory factory, Connection conn, Session sess ) {
        _connectionFactory = factory;
        _connection = conn;
        _session = sess;
        _jndiContext = context;
    }

    public ConnectionFactory getConnectionFactory() {
        if ( _closed ) throw new IllegalStateException("Bag has been closed");
        return _connectionFactory;
    }

    public Connection getConnection() {
        if ( _closed ) throw new IllegalStateException("Bag has been closed");
        return _connection;
    }

    public Session getSession() {
        if ( _closed ) throw new IllegalStateException("Bag has been closed");
        return _session;
    }

    public Context getJndiContext() {
        if ( _closed ) throw new IllegalStateException("Bag has been closed");
        return _jndiContext;
    }

    public void close() {
        try {
            try {
                if ( _session != null ) _session.close();
            } catch ( JMSException e ) {
            } finally {
                _session = null;
            }

            try {
                if ( _connection != null ) _connection.stop();
            } catch ( JMSException e ) {
            }

            try {
                if ( _connection != null ) _connection.close();
            } catch ( JMSException e ) {
            } finally {
                _connection = null;
            }

            try {
                if ( _jndiContext != null ) _jndiContext.close();
            } catch ( NamingException e ) {
            } finally {
                _jndiContext = null;
            }
        } finally {
            _closed = true;
        }
    }

    protected void finalize() throws Throwable {
        try {
            if ( !_closed ) {
                _logger.warning( "JmsBag finalized" );
                close();
            }
        } finally {
            super.finalize();
        }
    }

    private static final Logger _logger = LogManager.getInstance().getSystemLogger();
    private ConnectionFactory _connectionFactory;
    private Connection _connection;
    private Session _session;
    private Context _jndiContext;
    private volatile boolean _closed = false;
}
