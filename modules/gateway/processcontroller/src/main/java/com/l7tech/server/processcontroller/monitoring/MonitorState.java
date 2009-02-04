/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.api.monitoring.Monitorable;

import java.io.Closeable;
import java.util.Set;

/**
 * A holder for monitoring state.
 */
abstract class MonitorState<MT extends Monitorable> implements Closeable {
    protected final MT monitorable;
    protected final String componentId;
    protected final Set<Long> triggerOids;

    protected MonitorState(MT monitorable, String componentId, Set<Long> triggerOids) {
        this.monitorable = monitorable;
        this.componentId = componentId;
        this.triggerOids = triggerOids;
    }

    public MT getMonitorable() {
        return monitorable;
    }

    public String getComponentId() {
        return componentId;
    }

    public Set<Long> getTriggerOids() {
        return triggerOids;
    }

    /**
     * Tells the state that it can discard any monitoring information older than the specified timestamp.
     *
     * @param retainNewerThan the time before which old data can be deleted
     */
    abstract void expireHistory(Long retainNewerThan);
}
