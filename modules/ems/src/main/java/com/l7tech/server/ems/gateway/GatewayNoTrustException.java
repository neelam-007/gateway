package com.l7tech.server.ems.gateway;

/**
 * Exception thrown if a Gateway node or cluster cannot be accessed due to bad trust status.
 */
public class GatewayNoTrustException extends GatewayException {

    public GatewayNoTrustException() {
    }

    public GatewayNoTrustException(String message) {
        super(message);
    }

}