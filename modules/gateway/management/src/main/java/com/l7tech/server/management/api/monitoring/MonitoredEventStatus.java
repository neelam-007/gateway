/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.objectmodel.Goid;

import java.util.Set;

/**
 * Reports on the number of times a particular event has been observed during some time period. 
 */
public class MonitoredEventStatus extends MonitoredStatus {
    private int count;
    private long period;

    @Deprecated // XML only
    protected MonitoredEventStatus() {
    }

    public MonitoredEventStatus(MonitorableEvent monitorable, String componentId, StatusType status, long timestamp, Set<Goid> triggerGoids, int count, long period) {
        super(monitorable.getComponentType(), monitorable.getName(), componentId, timestamp, status, triggerGoids);
        this.count = count;
        this.period = period;
    }

    /** The number of events that has been observed during {@link #period} */
    public int getCount() {
        return count;
    }

    /** The length of time during which {@link #count} events were observed */
    public long getPeriod() {
        return period;
    }

    @Deprecated // XML only
    protected void setCount(int count) {
        this.count = count;
    }

    @Deprecated // XML only
    protected void setPeriod(long period) {
        this.period = period;
    }
}
