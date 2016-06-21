package com.l7tech.server.processcontroller;

import java.io.IOException;

/**
 * Interface for accessing patcher properties
 */
public interface PatcherProperties {
    /**
     * Property to enable or disable auto deletion of *.L7P after installation
     */
    public static final String PROP_L7P_AUTO_DELETE = "patcher.l7p.auto.delete";
    /**
     * {@link #PROP_L7P_AUTO_DELETE} default value if missing.
     */
    public static final boolean PROP_L7P_AUTO_DELETE_DEFAULT_VALUE = true;

    /**
     * Get a {@code String} value from patcher.properties.
     *
     * @param propertyName    the property name.  Required and cannot be {@code null}.
     * @param defaultValue    default value to use if property is not configured or is invalid.
     * @return The property value, or the default
     * @throws IOException if an error occurs while accessing the patcher.properties file.
     */
    String getProperty(String propertyName, String defaultValue) throws IOException;

    /**
     * set a property in patcher.properties.
     *
     * @param propertyName     the property name.  Required and cannot be {@code null}.
     * @param propertyValue    the property value.  Required and cannot be {@code null}.
     * @return The property previous value or {@code null} if it did not have one.
     * @throws IOException if an error occurs while accessing the patcher.properties file.
     */
    String setProperty(String propertyName, String propertyValue) throws IOException;

    /**
     * Get a {@code int} value from patcher.properties.
     *
     * @param propertyName    the property name.  Required and cannot be {@code null}.
     * @param defaultValue    default value to use if property is not configured or is invalid.
     * @return The property value, or the default
     * @throws IOException if an error occurs while accessing the patcher.properties file.
     */
    int getIntProperty(String propertyName, int defaultValue) throws IOException;

    /**
     * Get a {@code boolean} value from patcher.properties.
     *
     * @param propertyName    the property name.  Required and cannot be {@code null}.
     * @param defaultValue    default value to use if property is not configured or is invalid.
     * @return The property value, or the default
     * @throws IOException if an error occurs while accessing the patcher.properties file.
     */
    boolean getBooleanProperty(String propertyName, boolean defaultValue) throws IOException;
}
