/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.util.ComparisonOperator;

import javax.persistence.Entity;

/**
 * A trigger that directs the monitoring system to periodically sample some property of the subject component, and fire
 * when the property matches a trigger expression.
 *
 * @author alex
 */
@Entity
public class PropertyTrigger extends Trigger {
    private String propertyId;
    private int samplingInterval;
    private ComparisonOperator operator;
    private String triggerValue;

    /** The ID of the property that's being monitored */
    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
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
    public String getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PropertyTrigger that = (PropertyTrigger)o;

        if (samplingInterval != that.samplingInterval) return false;
        if (operator != null ? !operator.equals(that.operator) : that.operator != null) return false;
        if (propertyId != null ? !propertyId.equals(that.propertyId) : that.propertyId != null) return false;
        if (triggerValue != null ? !triggerValue.equals(that.triggerValue) : that.triggerValue != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (propertyId != null ? propertyId.hashCode() : 0);
        result = 31 * result + samplingInterval;
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        result = 31 * result + (triggerValue != null ? triggerValue.hashCode() : 0);
        return result;
    }
}
