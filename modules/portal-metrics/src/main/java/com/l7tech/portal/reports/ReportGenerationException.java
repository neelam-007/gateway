package com.l7tech.portal.reports;

/**
 * Thrown when an error occurs while attempting to generate a report.
 */
public class ReportGenerationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ReportGenerationException(final String msg, final Throwable error) {
        super(msg, error);
    }
}
