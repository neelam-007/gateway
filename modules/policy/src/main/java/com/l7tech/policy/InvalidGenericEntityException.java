package com.l7tech.policy;

import com.l7tech.objectmodel.FindException;

/**
 * Exception thrown if a generic entity cannot be decoded.
 */
public class InvalidGenericEntityException extends FindException {
    public InvalidGenericEntityException() {
    }

    public InvalidGenericEntityException(String message) {
        super(message);
    }

    public InvalidGenericEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
