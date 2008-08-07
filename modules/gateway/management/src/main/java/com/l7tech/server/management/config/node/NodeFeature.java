/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.server.management.config.Feature;

/** @author alex */
public abstract class NodeFeature extends Feature<NodeConfig, NodeFeatureType> {
    public NodeFeature(PCNodeConfig parent, NodeFeatureType tag) {
        super(parent, tag, tag.isUnique());
    }
}