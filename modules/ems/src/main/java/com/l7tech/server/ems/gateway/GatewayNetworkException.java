package com.l7tech.server.ems.gateway;

/**
 * Exception thrown if a Gateway node or cluster cannot be accessed due to a network (or SOAP) problem.
 */
public class GatewayNetworkException extends GatewayException {
    public GatewayNetworkException() {
    }

    public GatewayNetworkException(String message) {
        super(message);
    }

    public GatewayNetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public GatewayNetworkException(Throwable cause) {
        super(cause);
    }
}
