/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.host;

import com.l7tech.server.management.config.Feature;

/** @author alex */
public abstract class HostFeature extends Feature<HostConfig, HostFeatureType> {
    HostFeature(PCHostConfig parent, HostFeatureType tag) {
        super(parent, tag, tag.isUnique());
    }

    @Deprecated
    protected HostFeature() {
        super(null, null, false);
    }
}
