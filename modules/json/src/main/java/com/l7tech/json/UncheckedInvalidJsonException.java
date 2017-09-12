package com.l7tech.json;

/**
 * Wraps an @see InvalidJsonException with an unchecked exception
 */
public class UncheckedInvalidJsonException extends RuntimeException {

    public UncheckedInvalidJsonException(InvalidJsonException cause) {
        super(cause);
    }

    public InvalidJsonException getCause() {
        return (InvalidJsonException) super.getCause();
    }
}
