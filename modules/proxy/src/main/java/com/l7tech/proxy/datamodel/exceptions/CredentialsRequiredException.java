package com.l7tech.proxy.datamodel.exceptions;

/**
 * Exception thrown when it is discovered that a password is needed and the user should be prompted for one
 * and the overall operation tried again once credentials are available.
 */
public class CredentialsRequiredException extends RuntimeException {
    public CredentialsRequiredException() {
    }

    public CredentialsRequiredException(String message) {
        super(message);
    }

    public CredentialsRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public CredentialsRequiredException(Throwable cause) {
        super(cause);
    }
}
