/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;

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

    public MonitorableProperty(ComponentType componentType, String propertyName, Class<? extends Serializable> valueClass) {
        super(componentType, propertyName);
        this.valueClass = valueClass;
    }

    public Class<? extends Serializable> getValueClass() {
        return valueClass;
    }
}
