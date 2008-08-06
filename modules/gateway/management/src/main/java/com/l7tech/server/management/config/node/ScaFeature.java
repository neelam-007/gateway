/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/** @author alex */
public class ScaFeature extends ServiceNodeFeature {
    private final boolean scaOwner;

    ScaFeature(PCServiceNodeConfig parent, boolean scaOwner) {
        super(parent, NodeFeatureType.SCA);
        this.scaOwner = scaOwner;
    }

    public boolean isScaOwner() {
        return scaOwner;
    }
}
