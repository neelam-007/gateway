/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.Session;

/**
 * @author alex
 */
public class HibernatePersistenceContextStub extends HibernatePersistenceContext {
    public Session getSession() {
        return _session;
    }

    public HibernatePersistenceContextStub( Session session ) {
        super( session );
    }
}
