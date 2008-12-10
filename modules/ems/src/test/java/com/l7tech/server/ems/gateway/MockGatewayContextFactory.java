package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;

/**
 *
 */
public class MockGatewayContextFactory implements GatewayContextFactory {
    @Override
    public GatewayContext getGatewayContext(User user, String host, int port) throws GatewayException {
        return null;
    }

    @Override
    public GatewayContext getGatewayContext(User user, String mappingProp, String host, int port) throws GatewayException {
        return null;
    }
}
