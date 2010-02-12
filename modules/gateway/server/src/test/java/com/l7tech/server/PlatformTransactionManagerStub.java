package com.l7tech.server;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Stub PlatformTransactionManager for test use.
 */
public class PlatformTransactionManagerStub implements PlatformTransactionManager {

    //- PUBLIC

    @Override
    public TransactionStatus getTransaction( final TransactionDefinition transactionDefinition ) throws TransactionException {
        SimpleTransactionStatus sts = status.get();

        if ( sts == null || sts.isCompleted() ) {
            sts = new SimpleTransactionStatus();
            status.set(sts);
        } 

        return sts;
    }

    @Override
    public void commit( final TransactionStatus transactionStatus ) throws TransactionException {
        status.get().setCompleted();
    }

    @Override
    public void rollback( final TransactionStatus transactionStatus ) throws TransactionException {
        status.get().setCompleted();
    }

    //- PRIVATE

    private final ThreadLocal<SimpleTransactionStatus> status = new ThreadLocal<SimpleTransactionStatus>();
}
