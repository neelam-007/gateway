/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.node;

/** @author alex */
public class TarariFeature extends NodeFeature {
    private final boolean tarariOwner;

    TarariFeature(PCNodeConfig parent, boolean tarariOwner) {
        super(parent, NodeFeatureType.TARARI);
        this.tarariOwner = tarariOwner;
    }

    public boolean isTarariOwner() {
        return tarariOwner;
    }
}
