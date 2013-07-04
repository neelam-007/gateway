package com.l7tech.policy.assertion.ext.message;

/**
 * Thrown when the message data is not well formed.
 */
public class InvalidDataException extends Exception {
    private static final long serialVersionUID = -1785875013351638986L;

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(Throwable cause) {
        super(cause);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
