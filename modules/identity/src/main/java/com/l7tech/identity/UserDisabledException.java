package com.l7tech.identity;

/**
 * User: wlui
 */
public class UserDisabledException extends AuthenticationException {

    public UserDisabledException(String message) {
        super(message);
    }

    public UserDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
