/*
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: 4-Feb-2004
 * Time: 12:30:20 PM
 */
package com.l7tech.common.security.xml;

/**
 * This is the general security processor exception
 * The nature of the failure is described by the cause (crypto/signing
 * exception, XPath excpetion etc).
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SecurityProcessorException extends Exception {
    /**
     * Constructs a new exception with <code>null</code> as its detail message
     * and the cause is not initialized
     */
    public SecurityProcessorException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public SecurityProcessorException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * @param message the detail message
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).
     */
    public SecurityProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

}