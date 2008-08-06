/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.gateway;

import com.l7tech.server.management.config.Feature;

/** @author alex */
public abstract class GatewayFeature extends Feature<GatewayConfig, GatewayFeatureType> {
    GatewayFeature(PCGatewayConfig parent, GatewayFeatureType tag) {
        super(parent, tag, tag.isUnique());
    }

    @Deprecated
    protected GatewayFeature() {
        super(null, null, false);
    }
}
