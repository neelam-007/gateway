package com.l7tech.gateway.api;

/**
 * Base class for Gateway Management API runtime exceptions.
 */
@SuppressWarnings( { "serial" } )
public class ManagementRuntimeException extends RuntimeException {

    public ManagementRuntimeException( final String message ) {
        super( message );
    }

    public ManagementRuntimeException( final String message, final Throwable cause ) {
        super( message, cause );
    }

    public ManagementRuntimeException( final Throwable cause ) {
        super( cause );
    }
}
