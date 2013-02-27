package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms.JmsBag;

import javax.jms.*;
import javax.naming.Context;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
class JmsTaskBag extends JmsBag {
    
    JmsTaskBag( final JmsBag jmsBag ) {
        super(jmsBag.getJndiContext(), jmsBag.getConnectionFactory(), jmsBag.getConnection(), jmsBag.getSession() );
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        return super.getConnectionFactory();
    }

    @Override
    public Connection getConnection() {
        return super.getConnection();
    }

    @Override
    public Session getSession() {
        return super.getSession();
    }

    @Override
    public Context getJndiContext() {
        return super.getJndiContext();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        try {
            if ( !isClosed() && getSession() != null ) {
                try {
                    getSession().close();
                } catch ( Exception e ) {
                    _logger.log(Level.WARNING, "Exception while closing JmsBag (session)", e);
                }
            }
        } finally {
            closed = true;
            super.nullify();
        }
    }

    private static final Logger _logger = Logger.getLogger(JmsTaskBag.class.getName());
}