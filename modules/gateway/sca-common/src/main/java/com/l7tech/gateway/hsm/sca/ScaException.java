package com.l7tech.gateway.hsm.sca;

/**
 * Exception thrown if an SCA configuration operation fails.
 */
public class ScaException extends Exception {

    public ScaException() {
    }

    public ScaException(String message) {
        super(message);
    }

    public ScaException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScaException(Throwable cause) {
        super(cause);
    }
}
