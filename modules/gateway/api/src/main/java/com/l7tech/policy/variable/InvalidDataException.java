package com.l7tech.policy.variable;

/**
 * Thrown when the service request or response data is not well formed.
 */
public class InvalidDataException extends Exception {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(Throwable cause) {
        super(cause);
    }
}
