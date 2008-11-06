package com.l7tech.config.client;

/**
 * Exception class for invalid configuration state.
 * 
 * @author steve
 */
public class InvalidConfigurationStateException extends ConfigurationException {

    public InvalidConfigurationStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidConfigurationStateException(String message) {
        super(message);
    }

}
