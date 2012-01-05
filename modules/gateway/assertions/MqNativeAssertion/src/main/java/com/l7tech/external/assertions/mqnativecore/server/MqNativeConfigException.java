package com.l7tech.external.assertions.mqnativecore.server;

/**
 * This exception is thrown when a MQ native queue configuration is problematic.
 */
public class MqNativeConfigException extends Exception {
    public MqNativeConfigException(String message) {
        super( message );
    }

    public MqNativeConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
