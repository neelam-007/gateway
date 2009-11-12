package com.l7tech.uddi;

/**
 * UDDI Exception for network errors.
 */
public class UDDINetworkException extends UDDIException {
    public UDDINetworkException( final String message, final Throwable cause ) {
        super( message, cause );
    }
}
