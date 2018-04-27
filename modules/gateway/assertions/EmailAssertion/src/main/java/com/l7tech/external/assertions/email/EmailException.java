package com.l7tech.external.assertions.email;


/**
 * Exception thrown if there are any invalid scenarios while sending mail.
 */
public class EmailException extends Exception {

    /**
     * @param message message describing the exception
     */
    public EmailException(final String message) {
        super(message);
    }

    /**
     * @param message message describing the exception
     */
    public EmailException(final String message, final Throwable exception) {
        super(message, exception);
    }

}
