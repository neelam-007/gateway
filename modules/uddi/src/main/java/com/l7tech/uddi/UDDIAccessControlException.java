package com.l7tech.uddi;

/**
 * UDDI Exception for authentication and authorization errors.
 *
 * @author Steve Jones
 */
public class UDDIAccessControlException extends UDDIException {

    public UDDIAccessControlException(String message) {
        super(message);
    }
}
