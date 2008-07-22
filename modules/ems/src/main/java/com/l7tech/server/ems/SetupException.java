package com.l7tech.server.ems;

/**
 * Exception thrown if initial setup cannot be performed.
 */
public class SetupException extends Exception {
    public SetupException() {
    }

    public SetupException(String message) {
        super(message);
    }

    public SetupException(String message, Throwable cause) {
        super(message, cause);
    }

    public SetupException(Throwable cause) {
        super(cause);
    }
}
