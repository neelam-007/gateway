package com.l7tech.common.security.saml;

/**
 * The <code>InvalidAssertionException</code> is a specific SAML exception
 * that inidicates that the saml assertion is invalid.
 *
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class InvalidAssertionException extends SamlException {
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public InvalidAssertionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause
     *
     * @param cause the cause
     */
    public InvalidAssertionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidAssertionException(String message, Throwable cause) {
        super(message, cause);
    }
}
