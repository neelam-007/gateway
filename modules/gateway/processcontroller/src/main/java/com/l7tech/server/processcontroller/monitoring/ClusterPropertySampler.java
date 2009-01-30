package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;

import java.io.Serializable;

/**
 *
 */
public abstract class ClusterPropertySampler<V extends Serializable> extends PropertySampler<V> {
    protected ClusterPropertySampler(String componentId, String propertyName) {
        super(ComponentType.CLUSTER, componentId, propertyName);
    }
}
