package com.l7tech.portal.metrics;

public class SyncException extends RuntimeException {
    public SyncException(final String s) {
        super(s);
    }

    public SyncException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
