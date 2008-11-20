package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;

/**
 *
 */
public class MockGatewayTrustTokenFactory implements GatewayTrustTokenFactory {
    
    @Override
    public String getTrustToken(User user) throws GatewayException {
        return " ";
    }

    @Override
    public String getTrustToken() throws GatewayException {
        return " ";
    }
}
