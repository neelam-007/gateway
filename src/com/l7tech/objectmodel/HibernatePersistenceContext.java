/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.Session;
import cirrus.hibernate.Transaction;

/**
 * @author alex
 */
public class HibernatePersistenceContext implements PersistenceContext {
    Session getSession() {
        return _session;
    }

    Transaction getTransaction() {
        return _transaction;
    }

    public HibernatePersistenceContext( Session session ) {
        _session = session;
    }

    private Session _session;
    private Transaction _transaction;

    public void setTransaction( Transaction txn ) {
        _transaction = txn;
    }
}
