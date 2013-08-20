/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;

import java.util.Set;

public class PropertyCondition extends NotifiableCondition<MonitorableProperty> {
    private final Object value;

    protected PropertyCondition(MonitorableProperty monitorable, String componentId, InOut inOut, Long timestamp, Set<Goid> triggerGoids, Object value) {
        super(monitorable, componentId, inOut, timestamp, triggerGoids);
        this.value = value;
    }

    /**
     * The property value that caused the {@link com.l7tech.server.management.config.monitoring.PropertyTrigger} to fire. 
     */
    public Object getValue() {
        return value;
    }
}
