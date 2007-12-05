/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import com.l7tech.common.audit.Audit;
import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.common.audit.AuditDetailEvent;
import com.l7tech.common.audit.AuditDetail;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;
import java.util.logging.LogRecord;

public class Auditor implements Audit {
    private final ApplicationContext context;
    private final Object source;
    private final Logger logger;
    private final AuditLogListener listener;

    protected Auditor(Logger logger) {
        this.source = null;
        this.context = null;
        this.logger = logger;
        this.listener = null;
    }

    public Auditor(Object source, ApplicationContext context, Logger logger) {
        if(source  == null) throw new IllegalArgumentException("Event source is NULL. Cannot add AuditDetail to the audit record.");
        if(context == null) throw new IllegalArgumentException("ApplicationContext is NULL. Cannot add AuditDetail to the audit record.");

        this.source = source;
        this.context = context;
        this.logger = logger;
        this.listener = (AuditLogListener) context.getBean("auditLogListener", AuditLogListener.class);
    }

    public void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e) {
        context.publishEvent(new AuditDetailEvent(source, new AuditDetail(msg, params == null ? null : params, e)));

        if (logger == null) return;

        log(msg, params, e);
    }

    public void logAndAudit(AuditDetailMessage msg, String... params) {
        logAndAudit(msg, params, null);
    }

    public void logAndAudit(AuditDetailMessage msg) {
        logAndAudit(msg, null, null);
    }

    protected void log(AuditDetailMessage msg, String[] params, Throwable e) {
        if (logger.isLoggable(msg.getLevel())) {
            if ( listener != null ) {
                listener.notifyDetailCreated( logger.getName(), msg, params, e );
            } else {
                LogRecord record = new LogRecord(msg.getLevel(), msg.getMessage());
                record.setParameters(params);
                record.setThrown(e);
                record.setSourceClassName("");
                record.setSourceMethodName("");
                logger.log( record );
            }
        }
    }
}
