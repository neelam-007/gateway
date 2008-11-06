package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.beans.ConfigurationBean;

/**
 * 
 */
public class DynamicConfigurationBean<T> extends ConfigurationBean<T> {

    public DynamicConfigurationBean(String id, String name, T value) {
        super(id, name, value);
    }

    public DynamicConfigurationBean() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public ConfigResult onConfiguration( final T value, final ConfigurationContext context) {
        return ConfigResult.stay();
    }
    
}
