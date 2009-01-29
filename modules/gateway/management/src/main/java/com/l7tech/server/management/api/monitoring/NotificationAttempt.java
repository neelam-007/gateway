/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.util.ExceptionUtils;

/**
 * A record of an attempted notification
 */
public abstract class NotificationAttempt {
    private final StatusType statusType;
    private final long timestamp;
    private final String message;
    private final Throwable throwable;

    public NotificationAttempt(Throwable throwable, long timestamp) {
        this.statusType = StatusType.FAILED;
        this.timestamp = timestamp;
        this.message = ExceptionUtils.getMessage(throwable);
        this.throwable = throwable;
    }

    public NotificationAttempt(StatusType statusType, String message, long timestamp) {
        this.statusType = statusType;
        this.message = message;
        this.timestamp = timestamp;
        this.throwable = null;
    }

    public StatusType getStatus() {
        return statusType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getMessage() {
        return message;
    }

    public enum StatusType {
        IN_PROGRESS, // ?
        SENT,
        ACKNOWLEDGED,
        FAILED
    }
}
