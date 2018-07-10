package com.l7tech.common.io;

/**
 * Exception thrown by {@link SSLSocketFactoryWrapper} or {@link SSLServerSocketFactoryWrapper} if one of the specified
 * TLS version(s) is unavailable with the specified SSLSocketFactory or none of the specified TLS version(s) are supported
 * by the underlying TLS provider.
 */
public class UnsupportedTlsVersionsException extends RuntimeException {
    public UnsupportedTlsVersionsException(final String message) {
        super(message);
    }
    public UnsupportedTlsVersionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
