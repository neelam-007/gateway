/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: JmsBag.java 18643 2008-04-17 20:50:15Z megery $
 */

package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms.JmsBag;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.naming.Context;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class JmsTaskBag extends JmsBag {
    
    public JmsTaskBag( Context context, ConnectionFactory factory, Connection conn, Session sess ) {
        super(context, factory, conn, sess);
    }

    public ConnectionFactory getConnectionFactory() {
        return super.getConnectionFactory();
    }

    public Connection getConnection() {
        return super.getConnection();
    }

    public Session getSession() {
        return super.getSession();
    }

    public Context getJndiContext() {
        return super.getJndiContext();
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        try {
            if ( getSession() != null ) {
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