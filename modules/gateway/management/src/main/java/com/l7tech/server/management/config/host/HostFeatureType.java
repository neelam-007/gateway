/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.host;

/** @author alex */
public enum HostFeatureType {
    IP(false),
    TRUSTED_CERT(false);

    private final boolean unique;

    HostFeatureType(boolean unique) {
        this.unique = unique;
    }

    public boolean isUnique() {
        return unique;
    }
}
