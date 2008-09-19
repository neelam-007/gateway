package com.l7tech.console.util;

/**
 * Exception thrown when a remote invocation is canceled at the user's request.
 */
public class RemoteInvocationCanceledException extends RuntimeException {
    public RemoteInvocationCanceledException() {
    }

    public RemoteInvocationCanceledException(String message) {
        super(message);
    }

    public RemoteInvocationCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteInvocationCanceledException(Throwable cause) {
        super(cause);
    }
}
