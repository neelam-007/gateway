package com.l7tech.server.config.exceptions;

/**
 * User: megery
 * Date: Feb 4, 2008
 * Time: 1:22:37 PM
 */
public class ConfigException extends Exception{
    public ConfigException() {
        super();
    }

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
