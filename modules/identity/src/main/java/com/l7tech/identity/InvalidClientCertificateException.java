package com.l7tech.identity;

/**
 * @author alex
 */
public class InvalidClientCertificateException extends AuthenticationException {

    public InvalidClientCertificateException( String message ) {
        super( message );
    }

    public InvalidClientCertificateException( String message, Throwable cause ) {
        super( message, cause );
    }
}
