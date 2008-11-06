package com.l7tech.config.client;

/**
 * Exception class for configuration exceptions.
 * 
 * @author steve
 */
public class ConfigurationException extends Exception {

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(String message) {
        super(message);
    }

}
