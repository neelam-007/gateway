package com.l7tech.policy.assertion;

/**
 * Thrown when an error happens while trying to extract entity bytes from custom-key-value-store.
 */
@SuppressWarnings("UnusedDeclaration")
public class ReferenceEntityBytesException extends Exception {
    private static final long serialVersionUID = -5308661732410073877L;

    public ReferenceEntityBytesException(final String message) {
        super(message);
    }
    public ReferenceEntityBytesException(final Throwable cause) {
        super(cause);
    }
    public ReferenceEntityBytesException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
