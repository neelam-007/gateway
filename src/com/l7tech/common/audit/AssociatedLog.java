package com.l7tech.common.audit;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class AssociatedLog {

    long timeStamp;
    String message;
    String severity;
    int ordinal;

    public AssociatedLog(long timeStamp, String severity, String message, int ordinal) {
        this.timeStamp = timeStamp;
        this.severity = severity;
        this.message = message;
        this.ordinal = ordinal;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getMessage() {
        return message;
    }

    public String getSeverity() {
        return severity;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
