package com.l7tech.config.client;

/**
 * Interface to permit initialization of option values.
 */
public interface OptionInitializer {

    /**
     * Get the initial value for the named configuration option.
     *
     * @param configName The name of the configuration option.
     * @return The initial value, or null for none
     */
    Object getInitialValue( final String configName );
}
