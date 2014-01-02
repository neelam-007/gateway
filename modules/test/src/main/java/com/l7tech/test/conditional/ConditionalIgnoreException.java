package com.l7tech.test.conditional;

/**
 * Thrown when new instance from the specified {@link ConditionalIgnore} annotation
 * {@link com.l7tech.test.conditional.ConditionalIgnore#condition() condition} fails.
 */
@SuppressWarnings("UnusedDeclaration")
public class ConditionalIgnoreException extends RuntimeException {
    public ConditionalIgnoreException(final String message) {
        super(message);
    }

    public ConditionalIgnoreException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConditionalIgnoreException(final Throwable cause) {
        super(cause);
    }
}
