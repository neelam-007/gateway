/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.Session;

import javax.transaction.UserTransaction;
import javax.naming.InitialContext;

/**
 * @author alex
 */
public class HibernatePersistenceContext extends PersistenceContext {
    Session getSession() {
        return _session;
    }

    public HibernatePersistenceContext( Session session ) {
        _session = session;
    }

    public void commitTransaction() throws TransactionException {
        try {
            if ( _session != null )
                _session.flush();
            else
                throw new IllegalStateException( "Can't commit when there's no session!" );
            getUserTransaction().commit();
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
        try {
            return (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        }
    }

    public void beginTransaction() throws TransactionException {
        try {
            getUserTransaction().begin();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        }
    }

    public void rollbackTransaction() throws TransactionException {
        try {
            getUserTransaction().rollback();
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

    protected Session _session;
}
