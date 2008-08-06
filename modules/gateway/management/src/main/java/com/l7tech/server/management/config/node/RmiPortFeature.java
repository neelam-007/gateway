/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/** @author alex */
public class RmiPortFeature extends ServiceNodeFeature {
    private final int rmiPort;

    public RmiPortFeature(PCServiceNodeConfig parent, int rmiPort) {
        super(parent, NodeFeatureType.RMI_PORT);
        this.rmiPort = rmiPort;
    }

    public int getRmiPort() {
        return rmiPort;
    }
}
