package com.l7tech.server.saml;

/**
 * The unchecked exception <code>SamlException</code> signals that the saml operation
 * could not be carried out.
 *
 * Note that this exception (and its subclasses) is not raised on errors within the SAML
 * problem domain, for example, subject not authorized to access aesource in an authorization
 * query.
 *
 * If this is in the context of the request it will typically result in the SOAP fault
 * code sent to the requestor (as per SAML SOAP binding spec).
 *
 * @author emil
 * @version 28-Jul-2004
 */
public class SamlException extends RuntimeException {
    /**
     * Constructs a new saml exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public SamlException(String message) {
        super(message);
    }

    /**
     * Constructs a new saml exception with the specified cause
     *
     * @param cause the cause
     */
    public SamlException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new saml exception with the specified detail message and
     * cause.
     * @param message the detail message
     * @param cause   the cause
     */
    public SamlException(String message, Throwable cause) {
        super(message, cause);
    }
}
