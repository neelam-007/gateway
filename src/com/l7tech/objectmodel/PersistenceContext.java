package com.l7tech.objectmodel;

import java.sql.SQLException;

/**
 * @author alex
 */
public abstract class PersistenceContext {
    public abstract void close() throws SQLException, ObjectModelException;
    public abstract void beginTransaction() throws TransactionException;
    public abstract void commitTransaction() throws TransactionException;
    public abstract void rollbackTransaction() throws TransactionException;

    public static PersistenceContext getCurrent() throws SQLException {
        PersistenceContext context = (PersistenceContext)_contextLocal.get();
        if ( context == null ) {
            context = PersistenceManager.getContext();
            _contextLocal.set( context );
        }
        return context;
    }

    static ThreadLocal _contextLocal = new ThreadLocal();
}
