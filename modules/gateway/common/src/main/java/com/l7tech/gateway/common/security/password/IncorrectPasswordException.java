package com.l7tech.gateway.common.security.password;

/**
 * Exception thrown when a password does not match the expected hashed value.
 */
public class IncorrectPasswordException extends Exception {
    public IncorrectPasswordException() {
        super("Incorrect password");
    }
}
