package com.l7tech.objectmodel;

import java.sql.SQLException;

/**
 * Manages transactions and sessions.
 *
 * @author alex
 */
public abstract class PersistenceContext {
    /**
     * commits currently open session
     * must be followed by a call to close() to release connection
     * @throws ObjectModelException
     */
    public abstract void flush() throws ObjectModelException;

    /**
     * rollbacks existing transaction if exists and closes the connection
     */
    public abstract void close();
    public abstract void beginTransaction() throws TransactionException;
    public abstract void commitTransaction() throws TransactionException;
    public abstract void rollbackTransaction() throws TransactionException;

    public static PersistenceContext getCurrent() throws SQLException {
        PersistenceContext context = (PersistenceContext)_contextLocal.get();
        if ( context == null ) {
            context = PersistenceManager.makeContext();
            _contextLocal.set( context );
        }
        return context;
    }

    static ThreadLocal _contextLocal = new ThreadLocal();
}
