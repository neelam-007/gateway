package com.l7tech.server.util;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Selects the data source for use with a given method.
 */
public class UserSwitchedRoutingDataSourceSelector implements MethodInterceptor {

    //- PUBLIC

    /**
     *
     */
    public UserSwitchedRoutingDataSourceSelector( final String dataSourceKey ) {
        this.dataSourceKey = dataSourceKey;
    }

    /**
     *
     */
    public Object invoke( final MethodInvocation methodInvocation ) throws Throwable {
        try {
            UserSwitchedRoutingDataSource.setDataSourceKey( dataSourceKey );
            return methodInvocation.proceed();
        } finally {
            UserSwitchedRoutingDataSource.setDataSourceKey( null ); 
        }
    }

    //- PRIVATE

    private final String dataSourceKey;

}
