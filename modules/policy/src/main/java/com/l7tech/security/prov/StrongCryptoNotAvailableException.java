package com.l7tech.security.prov;

/**
 * Utility exception that can be used to report that an operation cannot proceed because strong cryptography
 * does not appear to be enabled in the current JVM.
 */
public class StrongCryptoNotAvailableException extends Exception {
    public StrongCryptoNotAvailableException() {
    }

    public StrongCryptoNotAvailableException(String msg) {
        super(msg);
    }

    public StrongCryptoNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public StrongCryptoNotAvailableException(Throwable cause) {
        super(cause);
    }
}
