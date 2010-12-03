package com.l7tech.server.secureconversation;

/**
 *
 */
public class SessionCreationException extends Exception {

    public SessionCreationException( final String message ) {
        super( message );
    }

    public SessionCreationException( final String message, final Throwable cause ) {
        super( message, cause );
    }
}
