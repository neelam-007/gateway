/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.util.ComparisonOperator;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A trigger that directs the monitoring system to periodically sample some property of the subject component, and fire
 * when the property matches a trigger expression.
 *
 * @author alex
 */
@Entity
@XmlRootElement(name="propertyTrigger", namespace="http://ns.l7tech.com/secureSpan/1.0/monitoring")
public class PropertyTrigger extends Trigger<MonitorableProperty> {
    private String propertyName;
    private Long maxSamplingInterval;
    private ComparisonOperator operator;
    private String triggerValue;

    @Deprecated
    public PropertyTrigger() {
    }

    public PropertyTrigger(MonitorableProperty property, String componentId, ComparisonOperator operator, String triggerValue, long maxSamplingInterval) {
        super(property, componentId);
        this.propertyName = property.getName();
        this.maxSamplingInterval = maxSamplingInterval;
        this.operator = operator;
        this.triggerValue = triggerValue;
    }

    /** The name of the property that's being monitored */
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /** The interval, in milliseconds, between successive samples */
    public Long getMaxSamplingInterval() {
        return maxSamplingInterval;
    }

    public void setMaxSamplingInterval(Long maxSamplingInterval) {
        this.maxSamplingInterval = maxSamplingInterval;
    }

    /** The operator to use in comparing the sampled property value against the {@link #triggerValue} */
    public ComparisonOperator getOperator() {
        return operator;
    }

    public void setOperator(ComparisonOperator operator) {
        this.operator = operator;
    }

    /** The value to compare the property value with to determine whether the trigger should fire */
    public String getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    /**
     * {@link #propertyName} cannot be changed; other properties are OK
     */
    @Override
    public boolean isIncompatibleWith(Trigger that) {
        return super.isIncompatibleWith(that) ||
                !this.getPropertyName().equals(((PropertyTrigger)that).getPropertyName());
    }
}
