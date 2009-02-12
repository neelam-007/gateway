package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.server.ems.enterprise.SsgNode;

/**
 *
 */
public class MockGatewayContextFactory implements GatewayContextFactory {
    @Override
    public GatewayContext createGatewayContext(User user, String mappingProp, String gatewayHostname, int gatewayPort) throws GatewayException {
        return null;
    }

    @Override
    public ProcessControllerContext createProcessControllerContext(SsgNode node) throws GatewayException {
        return null;
    }
}
