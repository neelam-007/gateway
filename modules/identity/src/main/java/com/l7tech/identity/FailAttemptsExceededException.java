package com.l7tech.identity;

/**
 * User: dlee
 * Date: Jun 25, 2008
 */
public class FailAttemptsExceededException extends AuthenticationException {
    public FailAttemptsExceededException() {
    }

    public FailAttemptsExceededException(String message) {
        super(message);
    }

    public FailAttemptsExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
