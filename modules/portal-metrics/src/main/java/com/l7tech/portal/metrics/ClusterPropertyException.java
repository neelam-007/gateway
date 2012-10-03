package com.l7tech.portal.metrics;

public class ClusterPropertyException extends RuntimeException {
    public ClusterPropertyException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public ClusterPropertyException(final String s) {
        super(s);
    }
}
