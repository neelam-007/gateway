package com.l7tech.server.util;

import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Perform standard connection customizations.
 *
 * <p>Allow configuration of:</p>
 *
 * <ul>
 *   <li>Transaction isolation level (default Connection.TRANSACTION_READ_COMMITTED)</li>
 *   <li>Auto commit setting (default false)</li>
 * </ul>
 */
public class StandardConnectionCustomizer implements ConnectionCustomizer {

    //- PUBLIC

    public StandardConnectionCustomizer() {        
    }

    public void onAcquire( final Connection connection, final String dataSourceId ) throws Exception {
    }

    public void onDestroy( final Connection connection, final String dataSourceId ) throws Exception {
    }

    public void onCheckOut( final Connection connection, final String dataSourceId ) throws Exception {
        connection.setAutoCommit( false );
        connection.setTransactionIsolation( Connection.TRANSACTION_READ_COMMITTED );
    }

    public void onCheckIn( final Connection connection, final String dataSourceId ) throws Exception {
    }

    public static void setAutoCommit( boolean autoCommit ) {
        logger.config("Database connection autocommit value set to '"+autoCommit+"'.");
        StandardConnectionCustomizer.autoCommit.set( autoCommit );
    }

    public static void setTransactionIsolation( int isolationLevel ) {
        logger.config("Database connection transaction isolation value set to '"+isolationLevel+"'.");
        StandardConnectionCustomizer.isolation.set( isolationLevel );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(StandardConnectionCustomizer.class.getName());

    private static final AtomicBoolean autoCommit = new AtomicBoolean( false );
    private static final AtomicInteger isolation = new AtomicInteger( Connection.TRANSACTION_READ_COMMITTED ); 
}
