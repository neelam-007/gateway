/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.config.monitoring.ComponentType;

import java.io.IOException;

/**
 *
 */
class NodeStateSampler extends PropertySampler<NodeStateType> {
    protected NodeStateSampler(ComponentType componentType, String componentId) {
        super(componentType, componentId, "nodeState");
    }

    public NodeStateType sample() throws PropertySamplingException {
        // TODO not clear if we want a PropertySampler to deal with this
        return NodeStateType.UNKNOWN;
    }

    public void close() throws IOException {
    }
}
