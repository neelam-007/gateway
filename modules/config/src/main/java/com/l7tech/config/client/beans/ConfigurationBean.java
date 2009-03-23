/**
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.config.client.beans;

import java.text.Format;
import java.text.ParseException;

/**
 * User: megery
 * Date: Aug 11, 2005
 * Time: 11:29:25 AM
 */
public class ConfigurationBean<T> {
    private String id;
    private String configName;
    protected T configValue;
    private Format formatter;
    private boolean deletable;

    public ConfigurationBean(String id, String name, T value) {
        this( id, name, value, null, false );
    }

    public ConfigurationBean( final String id,
                              final String configName,
                              final T configValue,
                              final Format formatter,
                              final boolean deletable ) {
        this.id = id;
        this.configName = configName;
        this.configValue = configValue;
        this.formatter = formatter;
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

    @SuppressWarnings({"unchecked"})
    public void processConfigValueInput( final String userInputConfigValue ) throws ParseException {
        if ( formatter != null ) {
            setConfigValue( (T)formatter.parseObject(userInputConfigValue) );
        } else {
            setConfigValue( (T)userInputConfigValue );
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Format getFormatter() {
        return formatter;
    }

    public void setFormatter(Format formatter) {
        this.formatter = formatter;
    }

    public boolean isDeletable() {
        return deletable;
    }

    /**
     * Override to get notified when you're deleted from the menu (should only happen if {@link #deletable})
     */
    public void onDelete() { }

    @Override
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigurationBean that = (ConfigurationBean) o;

        if (deletable != that.deletable) return false;
        if (configName != null ? !configName.equals(that.configName) : that.configName != null) return false;
        if (configValue != null ? !configValue.equals(that.configValue) : that.configValue != null) return false;
        if (formatter != null ? !formatter.equals(that.formatter) : that.formatter != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (configName != null ? configName.hashCode() : 0);
        result = 31 * result + (configValue != null ? configValue.hashCode() : 0);
        result = 31 * result + (formatter != null ? formatter.hashCode() : 0);
        result = 31 * result + (deletable ? 1 : 0);
        return result;
    }

    @Override
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
