package com.l7tech.policy.assertion.ext.store;

/**
 * Thrown if an error is encountered while invoking methods in {@link KeyValueStoreServices}.
 */
public class KeyValueStoreException extends RuntimeException {

    public KeyValueStoreException(String message) {
        super(message);
    }

    public KeyValueStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}