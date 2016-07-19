package com.l7tech.server;

import java.util.Properties;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public interface NodePropertiesLoader {
    Properties getProperties();

    String getProperty(String key, String defaultValue);

    boolean isDiskless();
}
