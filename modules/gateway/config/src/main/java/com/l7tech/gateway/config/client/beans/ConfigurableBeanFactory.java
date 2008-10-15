/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

/**
 * Used to add the option to make new ConfigurableBeans that might then be customized
 * @author alex
 */
public abstract class ConfigurableBeanFactory<T extends ConfigurationBean> extends ConfigurationBean {
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
}
