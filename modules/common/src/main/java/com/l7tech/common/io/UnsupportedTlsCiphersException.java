package com.l7tech.common.io;

/**
 * Exception thrown by {@link SSLSocketFactoryWrapper} or {@link SSLServerSocketFactoryWrapper} if none of the
 * specified TLS ciphers are supported by the underlying TLS provider.
 */
public class UnsupportedTlsCiphersException extends RuntimeException {
    public UnsupportedTlsCiphersException(final String message) {
        super(message);
    }
}
