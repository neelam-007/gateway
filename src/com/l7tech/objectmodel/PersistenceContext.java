package com.l7tech.objectmodel;

import javax.transaction.UserTransaction;

/**
 * @author alex
 */
public interface PersistenceContext {
    void beginTransaction() throws TransactionException;
    void commitTransaction() throws TransactionException;
    void rollbackTransaction() throws TransactionException;
}
