package com.ca.apim.gateway.extension.sharedstate;

import java.util.Properties;

/**
 * The Configuration allows user of various Provider to pass specific configuration to set config for
 * SharedKeyValueStore, SharedCounterStore, and SharedScheduledExecutorService and may hold additional configuration for
 * the specific provider
 */
public class Configuration {

    private final Properties properties;

    /**
     * Define common config params
     */
    public enum Param {
        PERSISTED("persisted", "false"),

        SCHEDULED_EXECUTOR_CORE_POOL_SIZE("schedule_executor_core_pool_size", "0");

        private final String name;
        private final String defaultValue;

        Param(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        private String getName() {
            return name;
        }

        private String getDefaultValue() {
            return defaultValue;
        }
    }

    public Configuration() {
        this.properties = new Properties();
    }

    public Configuration(Properties properties) {
        this.properties = (Properties) properties.clone();
    }

    public String get(Param param) {
        return properties.getProperty(param.getName(), param.getDefaultValue());
    }

    public String get(String name) {
        return properties.getProperty(name);
    }

    public Configuration set(String name, String value) {
        properties.setProperty(name, value);
        return this;
    }
}
