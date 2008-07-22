/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.event.admin;

import com.l7tech.gateway.common.cluster.ClusterProperty;

/**
 * Event fired when a Cluster Property is added, removed, or changed.
 */
public class ClusterPropertyEvent extends AdminEvent {
    public static final String ADDED = "added";
    public static final String CHANGED = "changed";
    public static final String REMOVED = "removed";

    private final String disposition;

    public ClusterPropertyEvent(ClusterProperty property, String disposition) {
        super(property, "property " + disposition);
        if (disposition == null) throw new NullPointerException("disposition must not be null");
        this.disposition = disposition;
    }

    public ClusterProperty getClusterProperty() {
        return (ClusterProperty)source;
    }

    public String getDisposition() {
        return disposition;
    }

    public String toString() {
        return this.getClass().getName() + " [" + source.getClass().getName() + " " + ((ClusterProperty)source).getName() + "]";
    }
}
