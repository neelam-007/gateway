/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.audit;

import org.springframework.context.ApplicationEvent;

/**
 * An {@link ApplicationEvent} that records the creation of an {@link AuditDetail}
 */
public class AuditDetailEvent extends ApplicationEvent {
    private final AuditDetail detail;
    private final Throwable exception;

    public AuditDetailEvent(Object source, AuditDetail detail, Throwable exception) {
        super(source);
        this.detail = detail;
        this.exception = exception;
    }

    public AuditDetail getDetail() {
        return detail;
    }

    public Throwable getException() {
        return exception;
    }
}
