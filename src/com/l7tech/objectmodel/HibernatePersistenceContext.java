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

import org.apache.log4j.Category;

/**
 * @author alex
 */
public class HibernatePersistenceContext extends PersistenceContext {
    Session getSession() {
        return _session;
    }

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
            _log.error(  "in finalize()", ome );
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
            _log.error( he );
            throw new ObjectModelException( he.getMessage(), he );
        } catch ( SQLException se ) {
            _log.error( se );
            throw new ObjectModelException( se.getMessage(), se );
        }
    }

    public void beginTransaction() throws TransactionException {
        try {
            if ( _session == null || !_session.isOpen() || !_session.isConnected() )
                _session = _manager.getSession();
            _htxn = _session.beginTransaction();
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

    protected Category _log = Category.getInstance( getClass() );
    protected HibernatePersistenceManager _manager;
    protected Session _session;
    protected DataSource _dataSource;
    protected Connection _conn;
    protected cirrus.hibernate.Transaction _htxn;
}
