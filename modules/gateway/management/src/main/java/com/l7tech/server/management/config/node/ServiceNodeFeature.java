/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

import com.l7tech.server.management.config.Feature;

/** @author alex */
public abstract class ServiceNodeFeature extends Feature<ServiceNodeConfig, NodeFeatureType> {
    public ServiceNodeFeature(PCServiceNodeConfig parent, NodeFeatureType tag) {
        super(parent, tag, tag.isUnique());
    }
}