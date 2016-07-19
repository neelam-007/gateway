package com.l7tech.server;

import java.util.Properties;

/**
 * Stub implementation of the NodePropertiesLoader for test usage.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class NodePropertiesLoaderStub implements NodePropertiesLoader {

    private Properties properties = new Properties();

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean isDiskless() {
        return false;
    }
}
