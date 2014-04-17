/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO would it ever be worthwhile to include the status of whatever notifications might have been attempted?
 */
public abstract class MonitoredStatus {
    protected long timestamp;
    protected ComponentType type;
    protected String monitorableId;
    protected String componentId;
    protected StatusType status;
    protected Set<String> triggerOids;

    protected MonitoredStatus(ComponentType type, final String monitorableId, String componentId, long timestamp, StatusType status, Set<Goid> triggerGoids) {
        this.type = type;
        this.monitorableId = monitorableId;
        this.componentId = componentId;
        this.timestamp = timestamp;
        this.triggerOids = convertTriggerIds(triggerGoids);
        this.status = status;
    }

    /**
     * Converts a set of trigger ids which could be longs, goids, or strings to a set of string ids
     *
     * @param triggerOids The ids to convert
     * @return The string version of the ids
     */
    private Set<String> convertTriggerIds(Set<?> triggerOids) {
        return new HashSet<>(Functions.map(triggerOids, new Functions.Unary<String, Object>() {
            @Override
            public String call(Object oid) {
                return oid.toString();
            }
        }));
    }

    @Deprecated // XML only
    protected MonitoredStatus() {
    }

    @XmlAttribute
    public StatusType getStatus() {
        return status;
    }

    @XmlAttribute
    public ComponentType getType() {
        return type;
    }

    @XmlAttribute
    public String getMonitorableId() {
        return monitorableId;
    }

    @XmlAttribute
    public String getComponentId() {
        return componentId;
    }

    @XmlAttribute
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * The OID(s) of the {@link com.l7tech.server.management.config.monitoring.PropertyTrigger}s that were responsible
     * for monitoring this property.
     */
    @XmlElementWrapper(name="triggerOids")
    public Set<String> getTriggerOids() {
        return triggerOids;
    }

    @Deprecated // XML only
    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Deprecated // XML only
    protected void setType(ComponentType type) {
        this.type = type;
    }

    @Deprecated // XML only
    protected void setMonitorableId(String monitorableId) {
        this.monitorableId = monitorableId;
    }

    @Deprecated // XML only
    protected void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    @Deprecated // XML only
    protected void setStatus(StatusType status) {
        this.status = status;
    }

    @Deprecated // XML only
    protected void setTriggerOids(Set<String> triggerOids) {
        this.triggerOids = triggerOids;
    }

    @XmlEnum(String.class)
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

    @Override
    public String toString() {
        return "MonitoredStatus{" +
                "timestamp=" + timestamp +
                ", type=" + type +
                ", monitorableId='" + monitorableId + '\'' +
                ", componentId='" + componentId + '\'' +
                ", status=" + status +
                ", triggerOids=" + triggerOids +
                '}';
    }
}
