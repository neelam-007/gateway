package com.l7tech.gateway.config.client;

/**
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
