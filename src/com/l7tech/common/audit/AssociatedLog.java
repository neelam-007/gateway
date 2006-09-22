package com.l7tech.common.audit;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class AssociatedLog {

    private final long timeStamp;
    private final String message;
    private final String severity;
    private final String exception;
    private final int ordinal;

    public AssociatedLog(long timeStamp, String severity, String message, String exception, int ordinal) {
        this.timeStamp = timeStamp;
        this.severity = severity;
        this.message = message;
        this.exception = exception;
        this.ordinal = ordinal;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getMessage() {
        return message;
    }

    public String getException() {
        return exception;
    }

    public String getSeverity() {
        return severity;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
