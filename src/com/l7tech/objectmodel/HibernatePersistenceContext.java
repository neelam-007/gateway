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
public class HibernatePersistenceContext extends PersistenceContext {

    public HibernatePersistenceContext(Session mainSession) {
        this.mainSession = mainSession;
    }
    public synchronized Session getSession()  {
        return mainSession;
    }

    private Session mainSession;
}
