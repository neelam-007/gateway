/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.util.ComparisonOperator;

import javax.persistence.Entity;
import java.io.Serializable;

/**
 * A trigger that directs the monitoring system to periodically sample some property of the subject component, and fire
 * when the property matches a trigger expression.
 *
 * @author alex
 */
@Entity
public class PropertyTrigger<T extends Serializable & Comparable> extends Trigger<MonitorableProperty> {
    private String propertyName;
    private long maxSamplingInterval;
    private ComparisonOperator operator;
    private T triggerValue;
    private Class<? extends Serializable> propertyValueClass;

    public PropertyTrigger(MonitorableProperty property, String componentId, ComparisonOperator operator, T triggerValue, long maxSamplingInterval) {
        super(property, componentId);
        this.propertyName = property.getName();
        this.maxSamplingInterval = maxSamplingInterval;
        this.operator = operator;
        this.triggerValue = triggerValue;
        this.propertyValueClass = triggerValue.getClass();
    }

    /** The name of the property that's being monitored */
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /** The interval, in milliseconds, between successive samples */
    public long getMaxSamplingInterval() {
        return maxSamplingInterval;
    }

    public void setMaxSamplingInterval(long maxSamplingInterval) {
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
    public T getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(T triggerValue) {
        this.triggerValue = triggerValue;
    }

    public Class<? extends Serializable> getValueClass() {
        return propertyValueClass;
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
