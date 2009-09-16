package com.l7tech.external.assertions.xmlsec.server;

/**
 * Exception thrown if an object cannot be gathered as credentials.
 */
class UnsupportedTokenTypeException extends Exception {
    public UnsupportedTokenTypeException() {
    }

    public UnsupportedTokenTypeException(String message) {
        super(message);
    }
}
