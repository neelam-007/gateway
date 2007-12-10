package com.l7tech.admin;

/**
 * Represents a Gateway error.
 *
 * @author: ghuang
 */
public class GatewayRuntimeException extends RuntimeAdminException {

    //- PUBLIC

    public GatewayRuntimeException() {
        this(MESSAGE);
    }

    public GatewayRuntimeException(final String message) {
        super(message);
    }

    public GatewayRuntimeException(final Throwable cause) {
        this(MESSAGE, cause);
    }

    public GatewayRuntimeException(final String message, final Throwable cause) {
        super(message, cause);
    }

    //- PRIVATE

    private static final String MESSAGE = "The SecureSpan Gateway error (eg, DB failure, etc.) occurred, please contact your gateway administrator.";
}
