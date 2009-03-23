/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.beans.ConfigurationBean;

/**
 * Used to add the option to make new ConfigurableBeans that might then be customized
 * @author alex
 */
public abstract class ConfigurableBeanFactory<T extends ConfigurationBean> extends ConfigurationBean {
    protected int consumedInstances;
    private final int min;
    private final int max;

    protected ConfigurableBeanFactory(String id, String name, int min, int max) {
        super(id, name, null, null, false);
        this.min = min;
        this.max = max;
    }

    /** Makes a new ConfigurationBean.  If the bean is a {@link ConfigurationBean} or a {@link EditableConfigurationBean} that is already valid, */
    public abstract T make();

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public synchronized int getConsumedInstances() {
        return consumedInstances;
    }

    public synchronized void setConsumedInstances(int consumedInstances) {
        this.consumedInstances = consumedInstances;
    }

    /**
     * "Consumes" one of the factory's instances
     */
    public synchronized void consume() {
        if (max == -1) return;
        if (++consumedInstances > max) throw new IllegalStateException("Maximum number of items reached");
    }

    /**
     * "Releases" one of the factory's instances
     */
    public synchronized void release() {
        if (min == -1 || consumedInstances == 0) return;
        if (--consumedInstances < min) throw new IllegalStateException("Minimum number of items reached");
    }
}
