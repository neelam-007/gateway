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

    public MonitorableProperty(ComponentType componentType, String propertyName, Class<? extends Serializable> valueClass) {
        this(componentType, propertyName, valueClass, null, null, null);
    }

    public MonitorableProperty(ComponentType componentType, String name, Class<? extends Serializable> valueClass, Long suggestedSamplingInterval, ComparisonOperator suggestedComparisonOperator, String suggestedComparisonValue) {
        super(componentType, name);
        this.valueClass = valueClass;
        this.suggestedSamplingInterval = suggestedSamplingInterval;
        this.suggestedComparisonOperator = suggestedComparisonOperator;
        this.suggestedComparisonValue = suggestedComparisonValue;
    }

    /** The class of values of this property */
    public Class<? extends Serializable> getValueClass() {
        return valueClass;
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
