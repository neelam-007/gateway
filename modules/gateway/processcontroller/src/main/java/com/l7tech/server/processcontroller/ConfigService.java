/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.gateway.GatewayConfig;
import com.l7tech.server.management.config.gateway.PCGatewayConfig;
import com.l7tech.server.management.config.node.ServiceNodeConfig;

/** @author alex */
public interface ConfigService {
    GatewayConfig getGateway();

    void updateGateway(PCGatewayConfig gateway);

    void addServiceNode(ServiceNodeConfig node);

    void updateServiceNode(ServiceNodeConfig node);
}
