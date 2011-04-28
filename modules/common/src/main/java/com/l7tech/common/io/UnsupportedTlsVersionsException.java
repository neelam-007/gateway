package com.l7tech.common.io;

/**
 * Exception thrown by SSLSocketFactoryWrapper if one of the specified TLS version(s) is unavailable
 * with the specified SSLSocketFactory.
 */
public class UnsupportedTlsVersionsException extends RuntimeException {
    public UnsupportedTlsVersionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
