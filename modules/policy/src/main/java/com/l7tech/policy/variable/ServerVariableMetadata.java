/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

public class ServerVariableMetadata extends VariableMetadata {
    private final boolean checkedOnlyOnBoot;
    private final int clusterPropertyCacheAge;
    private final String clusterPropertyName;
    private final Object defaultValue;
    private final String systemPropertyName;
    private final boolean setSystemProperty;
    private final String description;

    public ServerVariableMetadata(String name, String description, Object defaultValue, String systemProperty, boolean setSystemProperty, boolean onlyOnBoot, String clusterProperty, int cacheAge) {
        super(name, false, false, name, false);
        this.description = description;
        this.defaultValue = defaultValue;
        this.checkedOnlyOnBoot = onlyOnBoot;
        this.systemPropertyName = systemProperty;
        this.setSystemProperty = setSystemProperty;
        this.clusterPropertyCacheAge = cacheAge;
        this.clusterPropertyName = clusterProperty;
    }

    public boolean isCheckedOnlyOnBoot() {
        return checkedOnlyOnBoot;
    }

    public int getClusterPropertyCacheAge() {
        return clusterPropertyCacheAge;
    }

    public String getClusterPropertyName() {
        return clusterPropertyName;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getSystemPropertyName() {
        return systemPropertyName;
    }

    public boolean isSetSystemProperty() {
        return setSystemProperty;
    }

    public String getDescription() {
        return description;
    }
}
