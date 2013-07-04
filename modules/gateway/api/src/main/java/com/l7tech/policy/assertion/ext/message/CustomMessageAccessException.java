package com.l7tech.policy.assertion.ext.message;

/**
 * Thrown if an error happens while extracting or overwriting {@link CustomMessage} data.
 * Use {@link #getCause()} to extract the underplaying cause of the error.
 */
public class CustomMessageAccessException extends Exception {
    private static final long serialVersionUID = -621323549724703833L;

    public CustomMessageAccessException(final String message) {
        super(message);
    }

    public CustomMessageAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
