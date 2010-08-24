package com.l7tech.identity;

/**
 * @author alex
 */
public class BadCredentialsException extends AuthenticationException {

    public BadCredentialsException( String message ) {
        super( message );
    }

    public BadCredentialsException( String message, Throwable cause ) {
        super( message, cause );
    }
}
