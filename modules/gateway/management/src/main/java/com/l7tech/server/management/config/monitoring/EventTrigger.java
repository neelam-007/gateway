/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.server.management.api.monitoring.MonitorableEvent;

import javax.persistence.Basic;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * An trigger that directs the monitoring system to react when the subject component fires events of a particular type.
 *  
 * @author alex
 */
@XmlRootElement
public class EventTrigger extends Trigger<MonitorableEvent> {
    @Deprecated
    public EventTrigger() {
    }

    @Override
    protected MonitorableEvent buildMonitorable() {
        return new MonitorableEvent(componentType, monitorableId);
    }

    /** The number of times the event must be seen before the trigger fires.  Required; must be at least 1. */
    private int count;

    /**
     * The time period within which {@link #count} events must be observed before the trigger fires.  Optional; use null to
     * indicate that there's no minimum time period in which sufficient events must be observed to fire the trigger.
     */
    private Integer period;

    public EventTrigger(MonitorableEvent event, String componentId, int count, Integer period) {
        super(event.getComponentType(), componentId, event.getName());
        this.count = count;
        this.period = period;
    }

    @Basic(optional=false)
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EventTrigger that = (EventTrigger) o;

        if (count != that.count) return false;
        if (period != null ? !period.equals(that.period) : that.period != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + count;
        result = 31 * result + (period != null ? period.hashCode() : 0);
        return result;
    }
}
