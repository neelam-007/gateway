package com.l7tech.server.ems.gateway;

/**
 * Exception thrown when a Gateway node or cluster cannot be accessed.
 */
public class GatewayException extends Exception {
    public GatewayException() {
    }

    public GatewayException(String message) {
        super(message);
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public GatewayException(Throwable cause) {
        super(cause);
    }
}
