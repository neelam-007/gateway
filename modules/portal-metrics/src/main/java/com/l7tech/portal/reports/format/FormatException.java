package com.l7tech.portal.reports.format;

/**
 * Thrown when an error occurs during formatting.
 */
public class FormatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FormatException(final String s) {
        super(s);
    }

    public FormatException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
