package com.l7tech.common.io;

/**
 * Exeption thrown when an alias already exists in a keystore.
 * XXX Should this extend KeyStoreException?  Or maybe GeneralSecurityException?
 */
public class DuplicateAliasException extends Exception {
    public DuplicateAliasException() {
    }

    public DuplicateAliasException(String message) {
        super(message);
    }

    public DuplicateAliasException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateAliasException(Throwable cause) {
        super(cause);
    }
}
