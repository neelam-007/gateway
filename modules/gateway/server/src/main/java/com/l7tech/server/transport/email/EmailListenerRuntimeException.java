package com.l7tech.server.transport.email;

/**
 * An exception that can be thrown when an email listener has a problem receiving or processing a message.
 */
public class EmailListenerRuntimeException extends Exception {
    public EmailListenerRuntimeException( String message ) {
        super( message );
    }

    public EmailListenerRuntimeException(Throwable e) {
        super(e);
    }

    public EmailListenerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
