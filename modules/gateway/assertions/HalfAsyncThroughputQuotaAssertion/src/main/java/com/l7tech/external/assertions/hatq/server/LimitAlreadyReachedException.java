package com.l7tech.external.assertions.hatq.server;

/**
 *
 */
public class LimitAlreadyReachedException extends Exception {
    public LimitAlreadyReachedException() {
    }

    public LimitAlreadyReachedException(String message) {
        super(message);
    }

    public LimitAlreadyReachedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimitAlreadyReachedException(Throwable cause) {
        super(cause);
    }

    public LimitAlreadyReachedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
