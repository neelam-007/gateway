/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.util.ComparisonOperator;

import java.io.Serializable;

/**
 * A class of property that can be monitored on some type of component.
 * <p/>
 * Notes:<ul>
 * <li>the superclass's {@link #name} field holds the property name</li>
 * <li>the valueClass is intentionally excluded from equals/hashCode</li>
 * </ul>
 */
public class MonitorableProperty extends Monitorable {
    private final Class<? extends Serializable> valueClass;
    private final Long suggestedSamplingInterval;
    private final ComparisonOperator suggestedComparisonOperator;
    private final String suggestedComparisonValue;
    private final String valueUnit;

    /**
     *
     * @param componentType see {@link #getComponentType()}. Required.
     * @param name The short machine-readable property name, ie "operatingStatus".  Required.
     * @param valueClass the class to which all values of this property will belong, ie Long.class.  Required.
     * @param valueUnit the unit for the values, ie "KiB"; or null if not applicable to this property.
     * @param suggestedSamplingInterval    a default value for sampling frequency in milliseconds between sampler invocations, or null.
     * @param suggestedComparisonOperator  a default comparison operator to use when checking if the value is out of bounds, or null.
     * @param suggestedComparisonValue     a default value that is probably outside the bounds of normality for this property, or null.
     */
    public MonitorableProperty(ComponentType componentType, String name, Class<? extends Serializable> valueClass, String valueUnit, Long suggestedSamplingInterval, ComparisonOperator suggestedComparisonOperator, String suggestedComparisonValue) {
        super(componentType, name);
        this.valueClass = valueClass;
        this.valueUnit = valueUnit;
        this.suggestedSamplingInterval = suggestedSamplingInterval;
        this.suggestedComparisonOperator = suggestedComparisonOperator;
        this.suggestedComparisonValue = suggestedComparisonValue;
    }

    /** The class of values of this property */
    public Class<? extends Serializable> getValueClass() {
        return valueClass;
    }

    /** @return The unit for this property if applicable, or null. */
    public String getValueUnit() {
        return valueUnit;
    }

    /** A suggested sampling interval, in milliseconds */
    public Long getSuggestedSamplingInterval() {
        return suggestedSamplingInterval;
    }

    /** A suggested Comparison Operator */
    public ComparisonOperator getSuggestedComparisonOperator() {
        return suggestedComparisonOperator;
    }

    /** A suggested comparison value */
    public String getSuggestedComparisonValue() {
        return suggestedComparisonValue;
    }
}
