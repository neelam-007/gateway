package com.l7tech.common.io;

/**
 * Exception thrown if the desired alias is not found in a keystore file.
 * XXX Should this extend KeyStoreException?  Or maybe GeneralSecurityException?
 */
public class AliasNotFoundException extends Exception {
    public AliasNotFoundException() {
    }

    public AliasNotFoundException(String message) {
        super(message);
    }

    public AliasNotFoundException(Throwable cause) {
        super(cause);
    }

    public AliasNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
