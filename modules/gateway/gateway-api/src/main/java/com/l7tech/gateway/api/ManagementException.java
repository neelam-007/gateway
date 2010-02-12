package com.l7tech.gateway.api;

/**
 * Base class for Gateway Management API checked exceptions.
 */
@SuppressWarnings( { "serial" } )
public class ManagementException extends Exception {
        
    public ManagementException( final String message ) {
        super( message );
    }

    public ManagementException( final String message, final Throwable cause ) {
        super( message, cause );
    }

    public ManagementException( final Throwable cause ) {
        super( cause );
    }
}
