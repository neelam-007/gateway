/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.audit;

import org.springframework.context.ApplicationEvent;

/**
 * An {@link ApplicationEvent} that records the creation of an {@link AuditDetail}
 */
public class AuditDetailEvent extends ApplicationEvent {
    private final AuditDetail detail;

    public AuditDetailEvent(Object source, AuditDetail detail) {
        super(source);
        this.detail = detail;
    }

    public AuditDetail getDetail() {
        return detail;
    }
}
