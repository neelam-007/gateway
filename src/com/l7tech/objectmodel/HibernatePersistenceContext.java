/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.Session;
import cirrus.hibernate.HibernateException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import com.l7tech.logging.LogManager;

/**
 * @author alex
 */
public class HibernatePersistenceContext extends PersistenceContext {
    public HibernatePersistenceContext( Session session ) {
        _session = session;
        _manager = (HibernatePersistenceManager)PersistenceManager.getInstance();
    }

    public void commitTransaction() throws TransactionException {
        try {
            if ( _session == null )
                throw new IllegalStateException( "Can't commit when there's no session!" );
            else
                _session.flush();

            if ( _htxn != null ) _htxn.commit();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        } finally {
            try {
                //if ( _session != null ) _session.close();
                //_session = null;
                _htxn = null;
            } catch ( Exception e ) {
                throw new TransactionException( e.toString(), e );
            }
        }
    }

    public void finalize() {
        try {
            close();
        } catch ( ObjectModelException ome ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "in finalize()", ome);
        }
    }

    public void flush() throws ObjectModelException {
        try {
            if ( _htxn != null ) {
                _htxn.commit();
                _htxn = null;
            }
            if ( _session != null ) _session.flush();
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log( Level.SEVERE, "in flush()", se );
            throw new ObjectModelException( se.getMessage(), se );
        } catch ( HibernateException he ) {
            LogManager.getInstance().getSystemLogger().log( Level.SEVERE, "in flush()", he );
            throw new ObjectModelException( he.getMessage(), he );
        }
    }

    public void close() throws ObjectModelException {
        try {
            if ( _htxn != null ) {
                _htxn.rollback();
                _htxn = null;
            }
            if ( _session != null ) _session.close();
        } catch ( HibernateException he ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, he);
            throw new ObjectModelException( he.getMessage(), he );
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new ObjectModelException( se.getMessage(), se );
        }
    }

    public Session getSession() throws HibernateException, SQLException {
        if ( _session == null || !_session.isOpen() || !_session.isConnected() )
            _session = _manager.makeSession();
        return _session;
    }

    public void beginTransaction() throws TransactionException {
        try {
            _htxn = getSession().beginTransaction();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        }
    }

    public void rollbackTransaction() throws TransactionException {
        try {
            if ( _htxn != null ) _htxn.rollback();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        } finally {
            try {
                //if ( _session != null ) _session.close();
                //_session = null;
                _htxn = null;
            } catch ( Exception e ) {
                throw new TransactionException( e.toString(), e );
            }
        }
    }

    protected HibernatePersistenceManager _manager;
    protected Session _session;
    protected DataSource _dataSource;
    protected Connection _conn;
    protected cirrus.hibernate.Transaction _htxn;
}
