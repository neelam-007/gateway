/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import net.sf.hibernate.Session;

/**
 * @author alex
 */
public class HibernatePersistenceContextStub extends HibernatePersistenceContext {
    public Session getSession() {
        return mainSession;
    }

    public HibernatePersistenceContextStub( Session mainSession, Session auditSession ) {
        super(mainSession, auditSession);
    }
}
