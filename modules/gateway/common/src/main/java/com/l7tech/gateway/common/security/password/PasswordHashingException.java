package com.l7tech.gateway.common.security.password;

/**
 * Exception thrown if an error occurs while hashing a password.
 * <p/>
 * Throwers of this exception should take care to avoid revealing any password material either directly or
 * indirectly in the exception message or cause chain.
 */
public class PasswordHashingException extends Exception {
    public PasswordHashingException() {
    }

    public PasswordHashingException(String message) {
        super(message);
    }

    public PasswordHashingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PasswordHashingException(Throwable cause) {
        super(cause);
    }
}
