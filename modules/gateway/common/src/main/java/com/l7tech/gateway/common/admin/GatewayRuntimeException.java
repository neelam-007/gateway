package com.l7tech.gateway.common.admin;

/**
 * Represents a Gateway error.
 *
 * @author: ghuang
 */
public class GatewayRuntimeException extends RuntimeAdminException {

    /**
     * Standard error message
     */
    public static final String MESSAGE_DEFAULT = "A SecureSpan Gateway error occurred, please contact your administrator.";

    /**
     * Error message for transient issues
     */
    public static final String MESSAGE_TRANSIENT = "A SecureSpan Gateway error occurred, please try again.";

    /**
     * Create a runtime exception with the default error message.
     */
    public GatewayRuntimeException() {
        this(MESSAGE_DEFAULT);
    }

    /**
     *
     */
    public GatewayRuntimeException(final String message) {
        super(message);
    }

    /**
     * Create a runtime exception with the default error message.
     */
    public GatewayRuntimeException(final Throwable cause) {
        this(MESSAGE_DEFAULT, cause);
    }

    /**
     *
     */
    public GatewayRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
