package com.l7tech.common.security.saml;

/**
 * The <code>SamlException</code> represents the general SAML exception
 * that groups all the related exceptions.
 *
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SamlException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public SamlException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause
     *
     * @param cause the cause
     */
    public SamlException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public SamlException(String message, Throwable cause) {
        super(message, cause);
    }
}
