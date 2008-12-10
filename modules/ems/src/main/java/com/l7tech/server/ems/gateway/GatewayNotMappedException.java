package com.l7tech.server.ems.gateway;

/**
 * Exception thrown if a Gateway node or cluster cannot be accessed due to missing user mapping.
 */
public class GatewayNotMappedException extends GatewayException {
    
    public GatewayNotMappedException() {
    }

    public GatewayNotMappedException(String message) {
        super(message);
    }

}