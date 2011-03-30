package com.l7tech.identity;

/**
 * User: wlui
 */
public class FailInactivityPeriodExceededException extends AuthenticationException {

    public FailInactivityPeriodExceededException(String message) {
        super(message);
    }

    public FailInactivityPeriodExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
