/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.PCHostConfig;
import com.l7tech.server.management.config.node.NodeConfig;

/** @author alex */
public interface ConfigService {
    HostConfig getGateway();

    void updateGateway(PCHostConfig host);

    void addServiceNode(NodeConfig node);

    void updateServiceNode(NodeConfig node);
}
