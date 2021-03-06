package com.l7tech.server.util;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Extension of AbstractRoutingDataSource that allows user controlled routing.
 */
public class UserSwitchedRoutingDataSource extends AbstractRoutingDataSource {

    //- PUBLIC

    public static void setDataSourceKey( final Object key ){
        keyHolder.set( key );        
    }

    /**
     * TODO [jdk7] @Override
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
    
    //- PROTECTED

    @Override
    protected Object determineCurrentLookupKey() {
        return keyHolder.get();
    }

    //- PRIVATE

    private static final ThreadLocal<Object> keyHolder = new ThreadLocal<Object>();
}
