/**
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

/**
 * User: megery
 * Date: Aug 11, 2005
 * Time: 11:29:25 AM
 */
public class ConfigurationBean<T> {
    private String id;
    private String configName;
    protected T configValue;
    boolean deletable;

    public ConfigurationBean(String id, String name, T value) {
        this.id = id;
        this.configName = name;
        this.configValue = value;
    }

    public ConfigurationBean(String id, String configName, T configValue, boolean deletable) {
        this.id = id;
        this.configName = configName;
        this.configValue = configValue;
        this.deletable = deletable;
    }

    public ConfigurationBean() {
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public T getConfigValue() {
        return configValue;
    }

    public String getShortValueDescription() {
        return configValue == null ? null : configValue.toString();
    }

    public void setConfigValue(T configValue) {
        this.configValue = configValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public ConfigResult onConfiguration(T value, ConfigurationContext context) {
        return ConfigResult.stay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigurationBean bean = (ConfigurationBean)o;

        if (deletable != bean.deletable) return false;
        if (configName != null ? !configName.equals(bean.configName) : bean.configName != null) return false;
        if (configValue != null ? !configValue.equals(bean.configValue) : bean.configValue != null) return false;
        if (id != null ? !id.equals(bean.id) : bean.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (configName != null ? configName.hashCode() : 0);
        result = 31 * result + (configValue != null ? configValue.hashCode() : 0);
        result = 31 * result + (deletable ? 1 : 0);
        return result;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConfigurationBean[");
        builder.append("id=");
        builder.append(id);
        builder.append("; name=");
        builder.append(configName);
        builder.append("; value=");
        builder.append(configValue);
        builder.append(";]");
        return builder.toString();
    }
}
