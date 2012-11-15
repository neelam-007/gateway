package com.l7tech.objectmodel;

/**
 * Thrown when attempting to find an entity that is not supported.
 *
 * @author KDiep
 */
public class UnsupportedEntityTypeException extends FindException {
    public UnsupportedEntityTypeException() {
        super();
    }

    public UnsupportedEntityTypeException(String message) {
        this(message, null);
    }

    public UnsupportedEntityTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
