/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;

/**
 * An class of event that can be monitored on some type of component.
 * 
 * Note that the superclass's {@link #name} field holds the event ID.
 */
public class MonitorableEvent extends Monitorable {
    public MonitorableEvent(ComponentType componentType, String eventId) {
        super(componentType, eventId);
    }
}
