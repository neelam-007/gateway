package com.l7tech.external.assertions.mqnative.server;

/**
 * This exception is thrown when there's a MQ native problem.
 */
class MqNativeException extends Exception {
    public MqNativeException(String message) {
        super( message );
    }

    public MqNativeException(Throwable e) {
        super(e);
    }

    public MqNativeException(String message, Throwable cause) {
        super(message, cause);
    }
}