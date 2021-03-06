package com.l7tech.config.client.beans;

import com.l7tech.config.client.ConfigurationException;

import java.util.Collection;

/**
 * Implementations provide access to and storage of configurations.
 *
 * @author steve
 */
public interface ConfigurationBeanProvider {

    /**
     * Check if the provider is valid
     */
    boolean isValid();

    /**
     * Load a configuration from this provider.
     *
     * @return The collections of configurations (not null)
     * @throws com.l7tech.config.client.ConfigurationException If an error occurs
     */
    Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException;

    /**
     * Save a configuration to this provider.
     *
     * @param configuration The configuration to save
     * @throws ConfigurationException If an error occurs
     */
    void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException;
    
}
