package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * This is used to delay loading of a properties file untill it is needed.
 */
public interface PropertiesAccessor {
    /**
     * Return the properties file. This will load the properties file if it needs to be loaded.
     *
     * @return The properties file.
     * @throws ConfigurationException This is thrown if there was some error loading the properties file
     */
    @NotNull
    public Properties getProperties() throws ConfigurationException;
}
