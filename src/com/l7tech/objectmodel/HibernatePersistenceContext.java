/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.Session;

import javax.transaction.UserTransaction;
import javax.transaction.SystemException;
import javax.sql.DataSource;
import java.sql.Connection;

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

    public int getTransactionStatus() throws TransactionException {
        try {
            UserTransaction txn = getUserTransaction();
            return txn.getStatus();
        } catch ( SystemException se ) {
            throw new TransactionException( se.toString(), se );
        }
    }

    public void commitTransaction() throws TransactionException {
        try {
            if ( _session != null )
                _session.flush();
            else
                throw new IllegalStateException( "Can't commit when there's no session!" );

            _htxn.commit();
            //getUserTransaction().commit();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        } finally {
            try {
                if ( _session != null ) _session.close();
            } catch ( Exception e ) {
                throw new TransactionException( e.toString(), e );
            }
        }
    }

    protected UserTransaction getUserTransaction() throws TransactionException {
        throw new UnsupportedOperationException( "UserTransactions are no longer supported!");
        /*
        try {
            return (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        }
        */
    }

    public void beginTransaction() throws TransactionException {
        try {
            //getUserTransaction().begin();
            if ( _session == null || !_session.isOpen() )
                _session = _manager.getSession();
            _htxn = _session.beginTransaction();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        }
    }

    public void rollbackTransaction() throws TransactionException {
        try {
            //getUserTransaction().rollback();
            _htxn.rollback();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        } finally {
            try {
                if ( _session != null ) _session.close();
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
