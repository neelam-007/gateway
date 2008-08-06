/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.gateway;

/** @author alex */
public enum GatewayFeatureType {
    IP(false);

    private final boolean unique;

    GatewayFeatureType(boolean unique) {
        this.unique = unique;
    }

    public boolean isUnique() {
        return unique;
    }
}
