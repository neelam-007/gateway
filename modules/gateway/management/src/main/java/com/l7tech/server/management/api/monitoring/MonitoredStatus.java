/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import java.util.Set;

/**
 * TODO would it ever be worthwhile to include the status of whatever notifications might have been attempted?
 */
public abstract class MonitoredStatus<MT extends Monitorable> {
    protected final long timestamp;
    protected final MT monitorable;
    protected final StatusType status;
    protected final Set<Long> triggerOids;

    protected MonitoredStatus(long timestamp, Set<Long> triggerOids, StatusType status, MT monitorable) {
        this.timestamp = timestamp;
        this.triggerOids = triggerOids;
        this.status = status;
        this.monitorable = monitorable;
    }

    public StatusType getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /** The event or property that this status record reflects */
    public MT getMonitorable() {
        return monitorable;
    }

    /**
     * The OID(s) of the {@link com.l7tech.server.management.config.monitoring.PropertyTrigger}s that were responsible
     * for monitoring this property.
     */
    public Set<Long> getTriggerOids() {
        return triggerOids;
    }

    public enum StatusType {
        /** The monitorable is within tolerance */
        OK,

        /** The monitorable has no known state yet */
        UNKNOWN,

        /** The monitorable is out of the tolerance of one or more triggers, but no notification has been sent yet */
        WARNING,

        /** The monitorable is out of the tolerance of one or more triggers, and at least one notification has been sent. */
        NOTIFIED;

        public StatusType gt(StatusType that) {
            return this.ordinal() < that.ordinal() ? this : that;
        }
    }
}
