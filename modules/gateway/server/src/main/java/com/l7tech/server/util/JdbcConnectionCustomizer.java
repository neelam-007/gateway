package com.l7tech.server.util;

import com.ddtek.jdbc.extensions.ExtEmbeddedConnection;
import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Perform JDBC connection customizations.  Unlock connections using the data direct libraries
 *
 */
public class JdbcConnectionCustomizer implements ConnectionCustomizer {

    //- PUBLIC

    public JdbcConnectionCustomizer() {
    }

    @Override
    public void onAcquire( final Connection connection, final String dataSourceId ) throws Exception {
        if(connection instanceof ExtEmbeddedConnection){
            ExtEmbeddedConnection embeddedCon = (ExtEmbeddedConnection)connection;
            boolean unlocked = embeddedCon.unlock("Layer7!@Tech#$");
            if(!unlocked){
                logger.log( Level.WARNING, "Database connection unlock failed");
            }
        }
    }

    @Override
    public void onDestroy( final Connection connection, final String dataSourceId ) throws Exception {
    }

    @Override
    public void onCheckOut( final Connection connection, final String dataSourceId ) throws Exception {
    }

    @Override
    public void onCheckIn( final Connection connection, final String dataSourceId ) throws Exception {
    }
    //- PRIVATE

    private static final Logger logger = Logger.getLogger(JdbcConnectionCustomizer.class.getName());

}
