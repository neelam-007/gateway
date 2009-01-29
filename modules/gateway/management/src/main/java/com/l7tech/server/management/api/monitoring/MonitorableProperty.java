/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;

import java.io.Serializable;

/**
 * A class of property that can be monitored on some type of component.
 * 
 * Note that the superclass's {@link #name} field holds the property name.
 */
public class MonitorableProperty extends Monitorable {
    private final Class<? extends Serializable> valueClass;

    public MonitorableProperty(ComponentType componentType, String propertyName, Class<? extends Serializable> valueClass) {
        super(componentType, propertyName);
        this.valueClass = valueClass;
    }

    /** The class of this property's values */
    public Class<? extends Serializable> getValueClass() {
        return valueClass;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MonitorableProperty that = (MonitorableProperty) o;

        if (valueClass != null ? !valueClass.equals(that.valueClass) : that.valueClass != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (valueClass != null ? valueClass.hashCode() : 0);
        return result;
    }
}
