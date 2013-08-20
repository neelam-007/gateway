/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.api.monitoring.Monitorable;

import java.util.Set;

/**
 * Created by the {@link MonitoringKernel} when one or more triggers has fired.
 *
 * @param <MT> the type of Monitorable this condition signals
 */
abstract class NotifiableCondition<MT extends Monitorable> {
    private final MT monitorable;
    private final String componentId;
    private final Long timestamp;
    private final Set<Goid> triggerGoids;
    private final InOut inOut;

    protected NotifiableCondition(MT monitorable, String componentId, InOut inOut, Long timestamp, Set<Goid> triggerGoids) {
        this.monitorable = monitorable;
        this.inOut = inOut;
        this.componentId = componentId;
        this.timestamp = timestamp;
        this.triggerGoids = triggerGoids;
    }

    /**
     * The property or event that this condition resulted from
     */
    public MT getMonitorable() {
        return monitorable;
    }

    /** The time when the condition was detected */
    public Long getTimestamp() {
        return timestamp;
    }

    /** The OID(s) of the {@Link Trigger}s that were responsible for the condition */
    public Set<Goid> getTriggerGoids() {
        return triggerGoids;
    }

    /** The ID of the component that has the condition */
    public String getComponentId() {
        return componentId;
    }

    public InOut getInOut() {
        return inOut;
    }
}
