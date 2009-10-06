package com.l7tech.security.keys;

import java.security.KeyException;

/**
 * Exception thrown if an operation cannot be performed because the key type is not supported
 * (such as trying to do message level encryption for an EC public key).
 */
public class UnsupportedKeyTypeException extends KeyException {
    public UnsupportedKeyTypeException() {
    }

    public UnsupportedKeyTypeException(String msg) {
        super(msg);
    }

    public UnsupportedKeyTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedKeyTypeException(Throwable cause) {
        super(cause);
    }
}
