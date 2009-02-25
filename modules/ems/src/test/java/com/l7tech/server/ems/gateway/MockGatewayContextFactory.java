package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.server.ems.enterprise.SsgNode;

/**
 *
 */
public class MockGatewayContextFactory implements GatewayContextFactory {
    GatewayContext gatewayContext;
    ProcessControllerContext processControllerContext;

    public MockGatewayContextFactory() {
    }

    public MockGatewayContextFactory(GatewayContext gatewayContext, ProcessControllerContext processControllerContext) {
        this.gatewayContext = gatewayContext;
        this.processControllerContext = processControllerContext;
    }

    @Override
    public GatewayContext createGatewayContext(User user, String mappingProp, String gatewayHostname, int gatewayPort) throws GatewayException {
        return gatewayContext;
    }

    @Override
    public ProcessControllerContext createProcessControllerContext(SsgNode node) throws GatewayException {
        return processControllerContext;
    }

    public void setGatewayContext(GatewayContext gatewayContext) {
        this.gatewayContext = gatewayContext;
    }

    public void setProcessControllerContext(ProcessControllerContext processControllerContext) {
        this.processControllerContext = processControllerContext;
    }
}
