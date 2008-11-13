package com.l7tech.util;

/**
 * Interface for configuration access.
 */
public interface Config {

    /**
     * Get a String property by name.
     *
     * @param propertyName The name of the property.
     * @param defaultValue The default value for the property.
     * @return The value or the default value if not found.
     */
    String getProperty( String propertyName, String defaultValue );

    /**
     * Get a int property by name.
     *
     * @param propertyName The name of the property.
     * @param defaultValue The default value for the property.
     * @return The value or null if not found.
     */
    int getIntProperty( String propertyName, int defaultValue );

    /**
     * Get a long property by name.
     *
     * @param propertyName The name of the property.
     * @param defaultValue The default value for the property.
     * @return The value or null if not found.
     */
    long getLongProperty( String propertyName, long defaultValue );

    /**
     * Get a boolean property by name.
     *
     * @param propertyName The name of the property.
     * @param defaultValue The default value for the property.
     * @return The value or null if not found.
     */
    boolean getBooleanProperty( String propertyName, boolean defaultValue );
}
