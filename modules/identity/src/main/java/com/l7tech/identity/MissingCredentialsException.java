package com.l7tech.identity;

/**
 * @author alex
 */
public class MissingCredentialsException extends AuthenticationException {
    public MissingCredentialsException() {
        super( "Credentials not found" );
    }

    public MissingCredentialsException( String message ) {
        super( message );
    }

    public MissingCredentialsException( String message, Throwable cause ) {
        super( message, cause );
    }
}
