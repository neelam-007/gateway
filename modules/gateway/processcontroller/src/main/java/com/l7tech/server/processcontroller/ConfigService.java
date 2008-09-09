/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.node.NodeConfig;

import java.io.File;

/** @author alex */
public interface ConfigService {
    HostConfig getHost();

    void addServiceNode(NodeConfig node);

    void updateServiceNode(NodeConfig node);

    File getNodeBaseDirectory();

}
