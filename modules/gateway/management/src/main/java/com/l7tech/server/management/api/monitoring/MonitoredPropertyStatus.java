/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import java.io.Serializable;
import java.util.Set;

/**
 * The status of a particular property at some moment in time
 */
public abstract class MonitoredPropertyStatus extends MonitoredStatus<MonitorableProperty> {
    private final Serializable value;

    public MonitoredPropertyStatus(StatusType status, long timestamp, MonitorableProperty property, Set<Long> triggerOids, Serializable value) {
        super(timestamp, triggerOids, status, property);
        this.value = value;
    }

    public Serializable getValue() {
        return value;
    }

}
