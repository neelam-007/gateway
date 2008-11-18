/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.ConfigurationException;

/**
 * A ConfigurationBean that can be edited by a user.
 *
 * @author alex
 */
public abstract class EditableConfigurationBean<T> extends DynamicConfigurationBean<T> {
    private final T defaultValue;

    protected EditableConfigurationBean(String id, String shortIntro, T defaultValue) {
        super(id, shortIntro, null);
        this.defaultValue = defaultValue;
    }

    /**
     * The default value for this configurable.  If set to a non-null value, the user may proceed without modifying the
     * configurable.  If null, the user will need to enter a value.
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get the value to display to the user.
     *
     * @return The value or null for none.
     */
    public String getDisplayValue() {
        String value = null;

        if ( getConfigValue() != null ) {
            value = getConfigValue().toString();
        }

        return value;
    }

    /**
     * Called after a successfully {@link #parse parsed} value is entered so that implementations can
     * semantically validate the value.  If this method returns without throwing an exception, the value will be stored
     * in {@link #configValue}, and {@link #onConfiguration} will be called to find out how to proceed.
     *
     * @throws com.l7tech.config.client.ConfigurationException to indicate invalid data
     */
    public void validate(T value) throws ConfigurationException { }

    /**
     * Parse a line of user input.  Implementations must either return a value (which will then be stored in
     * {@link #configValue} before {@link #validate} is called) or throw a ConfigurationException.
     *
     * @param userInput a line of user input.
     * @return the value parsed from the user input.
     * @throws com.l7tech.config.client.ConfigurationException if the user's input is not valid.
     */
    public abstract T parse(String userInput) throws ConfigurationException;

}
