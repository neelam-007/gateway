package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;

/**
 * Retrieve a gateway context, which can obtain GatewayApi and NodeManagementApi.
 */
public interface GatewayContextFactory {
    GatewayContext getGatewayContext( User user, String host, int port ) throws GatewayException;
}
