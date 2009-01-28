/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;

/**
 * Thrown to indicate that a property sample could not be obtained.  Note that the individual identity of the original
 * Trigger is not retained, because the same property may be sampled on behalf of more than one trigger.
 */
public class PropertySamplingException extends Exception {
    private final ComponentType componentType;
    private final String componentId;
    private final String propertyName;

    public PropertySamplingException(PropertyTrigger trigger, String message, Throwable cause) {
        super(message, cause);
        this.componentType = trigger.getComponentType();
        this.componentId = trigger.getComponentId();
        this.propertyName = trigger.getPropertyName();
    }

    public PropertySamplingException(PropertyTrigger trigger, Throwable cause) {
        this(trigger, null, cause);
    }

    /** The type of the component whose property was to be sampled */
    public ComponentType getComponentType() {
        return componentType;
    }

    /** The ID of the component whose property was to be sampled */
    public String getComponentId() {
        return componentId;
    }

    /** The name of the property that was to be sampled */
    public String getPropertyName() {
        return propertyName;
    }
}
