/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.Session;

import javax.transaction.UserTransaction;
import javax.transaction.Status;

/**
 * @author alex
 */
public class HibernatePersistenceContextStub extends HibernatePersistenceContext {
    Session getSession() {
        return _session;
    }

    public HibernatePersistenceContextStub( Session session ) {
        super( session );
    }

    private class UserTransactionStub implements UserTransaction {
        public void rollback() {
            _status = Status.STATUS_ROLLING_BACK;
        }
        public void commit() {
            _status = Status.STATUS_COMMITTING;
        }
        public void begin() {
            _status = Status.STATUS_ACTIVE;
        }
        public void setRollbackOnly() {
            _status = Status.STATUS_MARKED_ROLLBACK;
        }

        public void setTransactionTimeout( int timeout ) { }
        public int getStatus() {
            return _status;
        }
        private int _status = Status.STATUS_NO_TRANSACTION;
    }

    protected UserTransaction getUserTransaction() throws TransactionException {
        try {
            return new UserTransactionStub();
        } catch ( Exception e ) {
            throw new TransactionException( e.toString(), e );
        }
    }
}
