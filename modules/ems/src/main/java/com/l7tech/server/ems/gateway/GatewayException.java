package com.l7tech.server.ems.gateway;

/**
 *
 */
public class GatewayException extends Exception {

    public GatewayException(final String message) {
        super(message);
    }

    public GatewayException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
