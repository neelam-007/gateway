/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import java.util.Set;

/**
 * Reports on the number of times a particular event has been observed during some time period. 
 */
public class MonitoredEventStatus extends MonitoredStatus<MonitorableEvent> {
    private final int count;
    private final long period;

    public MonitoredEventStatus(long timestamp, Set<Long> triggerOids, StatusType status, MonitorableEvent monitorable, int count, long period) {
        super(timestamp, triggerOids, status, monitorable);
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
}
