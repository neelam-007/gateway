package com.l7tech.server.config;

/**
 * User: megery
 * Date: Dec 3, 2007
 * Time: 4:25:44 PM
 */
public enum ConfigurationType {
    CONFIG_STANDALONE("Configure a standalone SSG"),
    CONFIG_CLUSTER("Configure a clustered SSG"),
    UNDEFINED("")
    ;

    private String description;

    ConfigurationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return description;
    }
}
