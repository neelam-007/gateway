package com.l7tech.proxy.datamodel.exceptions;

/**
 * Exception thrown when a queried-for SSG is not found.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 2:01:46 PM
 */
public class SsgNotFoundException extends Exception {
    public SsgNotFoundException() {
    }

    public SsgNotFoundException(final String message) {
        super(message);
    }

    public SsgNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SsgNotFoundException(final Throwable cause) {
        super(cause);
    }
}
