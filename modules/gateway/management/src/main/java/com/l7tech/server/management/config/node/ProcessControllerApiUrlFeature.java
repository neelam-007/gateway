/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/** @author alex */
public class ProcessControllerApiUrlFeature extends NodeFeature {
    private final String url;

    public ProcessControllerApiUrlFeature(PCNodeConfig parent, String url) {
        super(parent, NodeFeatureType.PC_NODE_API_URL);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
