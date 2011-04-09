package com.l7tech.common.password;

/**
 * Exception thrown when a password does not match the expected hashed value.
 */
public class IncorrectPasswordException extends Exception {
    public IncorrectPasswordException() {
        super("Incorrect password");
    }

    public IncorrectPasswordException(String message) {
        super(message);
    }
}
