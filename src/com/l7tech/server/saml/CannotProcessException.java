package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.RequestDocument;

/**
 * Signals that the receiver cannot process SAML request because of the problems with
 * the request.
 *
 *  This will typically result in the SOAP fault code sent to the requestor (as per
 * SAML SOAP binding spec).
 *
 * @author emil
 * @version 28-Jul-2004
 */
public class CannotProcessException extends SamlException {
    private RequestDocument request;

    /**
     * Constructs a new saml exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public CannotProcessException(String message, RequestDocument request) {
        super(message);
        this.request = request;
    }

    /**
     * Constructs a new saml exception with the specified cause
     *
     * @param cause the cause
     * @param cause the cause
     */
    public CannotProcessException(Throwable cause, RequestDocument request) {
        super(cause);
        this.request = request;
    }

    /**
     * Constructs a new saml exception with the specified detail message and
     * cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public CannotProcessException(String message, Throwable cause, RequestDocument request) {
        super(message, cause);
        this.request = request;
    }
}
