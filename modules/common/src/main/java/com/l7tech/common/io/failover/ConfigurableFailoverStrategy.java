package com.l7tech.common.io.failover;

import java.util.Map;

/**
 * Represent a failover Strategy is configurable. Properties is required for the strategy.
 */
public interface ConfigurableFailoverStrategy  {

    /**
     * Set the properties to the strategy
     * @param properties The properties of the strategy
     */
    void setProperties(Map properties);

    /**
     * A custom user interface to configure the strategy.
     * @return The Class name of the user interface, or null for the generic interface (Name/Value properties).
     */
    String getEditorClass();
    
}
