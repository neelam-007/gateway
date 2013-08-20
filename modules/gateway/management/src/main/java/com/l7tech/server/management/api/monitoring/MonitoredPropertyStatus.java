/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.config.monitoring.ComponentType;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import java.util.Set;

/**
 * The status of a particular property at some moment in time
 */
@XmlRootElement
public class MonitoredPropertyStatus extends MonitoredStatus {
    @XmlEnum
    public enum ValueType {
        /** The value was sampled correctly */
        OK,

        /** The PC has no record of a value for this property */
        NO_DATA_YET,

        /** The property could not be sampled correctly, the value might be stale */
        FAILED
    }

    private String value;
    private ValueType valueType;

    @Deprecated // XML only
    protected MonitoredPropertyStatus() {
    }

    public MonitoredPropertyStatus(ComponentType type, String monitorableId, String componentId, long timestamp, StatusType status, Set<Goid> triggerGoids, String value, ValueType valueType) {
        super(type, monitorableId, componentId, timestamp, status, triggerGoids);
        this.value = value;
        this.valueType = valueType;
    }

    public String getValue() {
        return value;
    }

    @Deprecated // XML only
    public void setValue(String value) {
        this.value = value;
    }

    @XmlAttribute
    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "MonitoredPropertyStatus{" +
                "value='" + value + '\'' +
                ", valueType=" + valueType +
                "} " + super.toString();
    }
}
