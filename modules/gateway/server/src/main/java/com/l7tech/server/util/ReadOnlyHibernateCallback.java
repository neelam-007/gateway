package com.l7tech.server.util;

import org.springframework.orm.hibernate3.HibernateCallback;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.FlushMode;

import java.sql.SQLException;

/**
 * @author alex
 */
public abstract class ReadOnlyHibernateCallback<RT> implements HibernateCallback<RT> {
    @Override
    public RT doInHibernate(Session session) throws HibernateException, SQLException {
        FlushMode old = session.getFlushMode();
        try {
            session.setFlushMode(FlushMode.MANUAL);
            return doInHibernateReadOnly(session);
        } finally {
            session.setFlushMode(old);
        }
    }

    protected abstract RT doInHibernateReadOnly(Session session) throws HibernateException, SQLException;
}
