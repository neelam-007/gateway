/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

/**
 * The status of a particular property at some moment in time
 */
@XmlRootElement
public class MonitoredPropertyStatus extends MonitoredStatus {
    private String value;

    @Deprecated // XML only
    protected MonitoredPropertyStatus() {
    }

    public MonitoredPropertyStatus(MonitorableProperty property, final String componentId, long timestamp, StatusType status, Set<Long> triggerOids, String value) {
        super(property.getComponentType(), property.getName(), componentId, timestamp, status, triggerOids);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Deprecated // XML only
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MonitoredPropertyStatus{" +
                "value='" + value + '\'' +
                "} " + super.toString();
    }
}
