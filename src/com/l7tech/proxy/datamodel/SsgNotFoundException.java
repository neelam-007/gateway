package com.l7tech.proxy.datamodel;

/**
 * Exception thrown when a queried-for SSG is not found.
 * User: mike
 * Date: Jun 5, 2003
 * Time: 2:01:46 PM
 */
public class SsgNotFoundException extends Exception {
    public SsgNotFoundException() {
    }

    public SsgNotFoundException(String message) {
        super(message);
    }

    public SsgNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsgNotFoundException(Throwable cause) {
        super(cause);
    }
}
