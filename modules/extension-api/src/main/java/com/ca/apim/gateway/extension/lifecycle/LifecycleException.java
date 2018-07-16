package com.ca.apim.gateway.extension.lifecycle;

/**
 * Exception that need to be thrown by any implementation of {@link LifecycleAwareExtension} if anything fails.
 */
public class LifecycleException extends Exception {

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
