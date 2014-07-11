package com.l7tech.external.assertions.xmppassertion.server;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * User: rseminoff
 * Date: 25/05/12
 *
 * Copied from com.l7tech.external.assertions.setclusterproperty.server.MockPlatformTransactionManager
 */
public class MockPlatformTransactionManager implements PlatformTransactionManager {

    @Override
    public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
        return null;
    }

    @Override
    public void commit(TransactionStatus transactionStatus) throws TransactionException { }

    @Override
    public void rollback(TransactionStatus transactionStatus) throws TransactionException { }
}
