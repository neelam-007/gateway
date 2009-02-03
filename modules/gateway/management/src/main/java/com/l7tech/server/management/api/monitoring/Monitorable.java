/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.config.monitoring.ComponentType;

/**
 * A monitorable aspect of some type of component.
 * <p/>
 * Instances should be read on startup into some registry, maybe from properties files or a hard-coded list that can be
 * shared between the EM and PC.
 */
public abstract class Monitorable {
    protected final ComponentType componentType;
    protected final String name;

    protected Monitorable(ComponentType componentType, String name) {
        this.componentType = componentType;
        this.name = name;
    }

    /** The type of component that has this property. */
    public ComponentType getComponentType() {
        return componentType;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Monitorable that = (Monitorable) o;

        if (componentType != that.componentType) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = componentType != null ? componentType.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public String toString() {
        return componentType + "." + name;
    }
}
