/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

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
public class PropertyTrigger<T extends Serializable> extends Trigger {
    private String propertyName;
    private int samplingInterval;
    private ComparisonOperator operator;
    private T triggerValue;
    private final Class<? extends T> triggerValueClass;

    public PropertyTrigger(Class<? extends T> triggerValueClass) {
        this.triggerValueClass = triggerValueClass;
    }

    /** The name of the property that's being monitored */
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /** The interval, in milliseconds, between successive samples */
    public int getSamplingInterval() {
        return samplingInterval;
    }

    public void setSamplingInterval(int samplingInterval) {
        this.samplingInterval = samplingInterval;
    }

    /** The operator to use in comparing the property value against the {@link #triggerValue} */
    public ComparisonOperator getOperator() {
        return operator;
    }

    public void setOperator(ComparisonOperator operator) {
        this.operator = operator;
    }

    /** The value to compare the property value against to determine whether the trigger should fire */
    public T getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(T triggerValue) {
        this.triggerValue = triggerValue;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PropertyTrigger that = (PropertyTrigger)o;

        if (samplingInterval != that.samplingInterval) return false;
        if (operator != null ? !operator.equals(that.operator) : that.operator != null) return false;
        if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null) return false;
        if (triggerValue != null ? !triggerValue.equals(that.triggerValue) : that.triggerValue != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (propertyName != null ? propertyName.hashCode() : 0);
        result = 31 * result + samplingInterval;
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        result = 31 * result + (triggerValue != null ? triggerValue.hashCode() : 0);
        return result;
    }

    /**
     * Two PropertyTrigger instances are incompatible if super.isIncompatibleWith(that) is true, or the {@link #propertyName} is
     * different.
     */
    @Override
    public boolean isIncompatibleWith(PropertyTrigger that) {
        return super.isIncompatibleWith(that) ||
                !this.getPropertyName().equals(that.getPropertyName());
    }

    public Class<? extends T> getTriggerValueClass() {
        return triggerValueClass;
    }
}
