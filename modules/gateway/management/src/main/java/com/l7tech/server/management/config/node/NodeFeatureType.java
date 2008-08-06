/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/** @author alex */
public enum NodeFeatureType {
    /** The feature describes the version of the ServiceNode */
    VERSION(true),
    TARARI(true),
    SCA(true),
    RMI_PORT(true),
    PC_NODE_API_PORT(true);

    private final boolean unique;

    private NodeFeatureType(boolean unique) {
        this.unique = unique;
    }

    public boolean isUnique() {
        return unique;
    }
}