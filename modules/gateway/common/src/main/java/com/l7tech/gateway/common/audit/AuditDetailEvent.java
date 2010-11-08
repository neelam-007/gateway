/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.audit;

import org.springframework.context.ApplicationEvent;

/**
 * An {@link ApplicationEvent} that records the creation of an {@link AuditDetail}
 * TODO this should be moved to its own separate event channel rather than the default ApplicationEvent channel.
 */
public class AuditDetailEvent extends ApplicationEvent {
    private final AuditDetail detail;
    private final Throwable exception;
    private final String loggerName;

    public AuditDetailEvent(Object source, AuditDetail detail, Throwable exception, String loggerName) {
        super(source);
        this.detail = detail;
        this.exception = exception;
        this.loggerName = loggerName;
    }

    public AuditDetail getDetail() {
        return detail;
    }

    public Throwable getException() {
        return exception;
    }

    /**
     * @return the name of the associated Logger, or null.
     */
    public String getLoggerName() {
        return loggerName;
    }
}
