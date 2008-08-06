/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/** @author alex */
public class ProcessControllerApiPortFeature extends ServiceNodeFeature {
    private final int pcApiPort;

    public ProcessControllerApiPortFeature(PCServiceNodeConfig parent, int pcApiPort) {
        super(parent, NodeFeatureType.PC_NODE_API_PORT);
        this.pcApiPort = pcApiPort;
    }

    public int getPcApiPort() {
        return pcApiPort;
    }
}