package com.l7tech.gateway.config.client.beans;

/**
 * User: megery
 * Date: Aug 11, 2005
 * Time: 11:29:25 AM
 */
public class ConfigurationBean {
    private String id;
    private String configName;
    private String configValue;

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
