package com.l7tech.server.transport;

/**
 *
 */
public final class ListenerException extends Exception {
    public ListenerException(String message) {
        super(message);
    }

    public ListenerException(String message, Throwable cause) {
        super(message, cause);
    }
}
