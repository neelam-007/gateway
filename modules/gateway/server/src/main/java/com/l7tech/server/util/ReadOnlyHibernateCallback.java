/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.util;

import org.springframework.orm.hibernate3.HibernateCallback;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.FlushMode;

import java.sql.SQLException;

/**
 * @author alex
 */
public abstract class ReadOnlyHibernateCallback implements HibernateCallback {
    public Object doInHibernate(Session session) throws HibernateException, SQLException {
        FlushMode old = session.getFlushMode();
        try {
            session.setFlushMode(FlushMode.NEVER);
            return doInHibernateReadOnly(session);
        } finally {
            session.setFlushMode(old);
        }
    }

    protected abstract Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException;
}
