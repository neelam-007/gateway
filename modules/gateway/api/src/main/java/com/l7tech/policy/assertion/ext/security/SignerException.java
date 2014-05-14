package com.l7tech.policy.assertion.ext.security;

/**
 * Thrown if an error is encountered while invoking methods in {@link com.l7tech.policy.assertion.ext.security.SignerServices}.
 */
public class SignerException extends RuntimeException {

    public SignerException(String message) {
        super(message);
    }

    public SignerException(String message, Throwable cause) {
        super(message, cause);
    }
}