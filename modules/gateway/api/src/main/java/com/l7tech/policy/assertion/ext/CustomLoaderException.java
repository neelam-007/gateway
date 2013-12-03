package com.l7tech.policy.assertion.ext;

/**
 * Exception thrown to indicate error during custom assertion module loading and unloading.
 */
public class CustomLoaderException extends Exception {
    public CustomLoaderException(final String message) {
        super(message);
    }

    public CustomLoaderException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
