/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.monitoring;

import com.l7tech.util.ExceptionUtils;

import javax.xml.bind.annotation.*;

/**
 * A record of an attempted notification
 */
@XmlRootElement
public class NotificationAttempt {
    private StatusType statusType;
    private long timestamp;
    private String message;
    private Throwable throwable;

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

    @Deprecated
    // XML only
    protected NotificationAttempt() {
    }

    @XmlAttribute(name = "status")
    public StatusType getStatus() {
        return statusType;
    }

    @XmlAttribute(name = "timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    @XmlTransient
    public Throwable getThrowable() {
        return throwable;
    }

    @XmlElement(name = "message")
    public String getMessage() {
        return message;
    }

    @Deprecated
    // Only for XML
    protected void setStatus(StatusType statusType) {
        this.statusType = statusType;
    }

    @Deprecated
    // Only for XML
    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Deprecated
    // Only for XML
    protected void setMessage(String message) {
        this.message = message;
    }

    @Deprecated
    // Only for XML
    protected void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @XmlType(name = "NotificationStatus")
    @XmlEnum(String.class)
    public enum StatusType {
        IN_PROGRESS, // ?
        SENT,
        ACKNOWLEDGED,
        FAILED
    }
}
