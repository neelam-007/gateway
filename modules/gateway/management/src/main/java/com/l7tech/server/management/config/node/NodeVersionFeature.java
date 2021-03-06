/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/** @author alex */
public class NodeVersionFeature extends NodeFeature {
    private final String version;

    public NodeVersionFeature(PCNodeConfig parent, String version) {
        super(parent, NodeFeatureType.VERSION);
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
