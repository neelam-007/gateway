/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Auditor implements Audit {
    private final ApplicationEventPublisher eventPub;
    private final Object source;
    private final Logger logger;
    private final AuditLogListener listener;
    private final AuditDetailFilter filter;

    /**
     * Create an Auditor that will only log and which has no message source.
     *
     * @param logger  logger to which details will be written.
     */
    protected Auditor(Logger logger) {
        this.source = null;
        this.eventPub = null;
        this.logger = logger;
        this.listener = null;
        this.filter = null;
    }

    /**
     * Create an Auditor that will only log.
     *
     * @param source  source object for audit events.  Required.
     * @param logger  logger to which details will be written.
     */
    public Auditor(Object source, Logger logger) {
        this(source, null, null, null, logger);
    }

    /**
     * Create an Auditor that will send details to the audit context and to the specified logger.
     *
     * @param source  source object for audit events.  Required.
     * @param context an ApplicationContext that will be used as the sink for audit detail events, or null.
     *                If this context provides "auditLogListener" and/or "auditDetailFilter" beans,
     *                they will be respected when details are audited.
     * @param logger  logger to which details will be written.
     */
    public Auditor(Object source, ApplicationContext context, Logger logger) {
        this(source, context, context, logger);
    }

    /**
     * Create an Auditor that will send details to the audit context and to the specified logger.
     *
     * @param source  source object for audit events.  Required.
     * @param beanFactory a BeanFactory to interrogate for "auditLogListener" and "auditDetailFilter" beans, or null.
     *                    If these beans are found, they will be respected when details are audited.
     * @param eventPub  an ApplicationEventPublisher that will be used as the sink for audit detail events.  Required.
     * @param logger  logger to which details will be written.
     */
    public Auditor(Object source, BeanFactory beanFactory, ApplicationEventPublisher eventPub, Logger logger) {
        this(source, findListener(beanFactory), findFilter(beanFactory), eventPub, logger);
    }

    private static AuditLogListener findListener(BeanFactory beanFactory) {
        if (beanFactory == null)
            return null;
        return beanFactory.containsBean("auditLogListener") ? (AuditLogListener) beanFactory.getBean("auditLogListener", AuditLogListener.class) : null;
    }

    private static AuditDetailFilter findFilter(BeanFactory beanFactory) {
        if (beanFactory == null)
            return null;
        return beanFactory.containsBean("auditDetailFilter") ? (AuditDetailFilter) beanFactory.getBean("auditDetailFilter", AuditDetailFilter.class) : null;
    }

    /**
     * Create an Auditor that will send details to the audit context and to the specified Logger.
     *
     * @param source  source object for audit events.  Required.
     * @param auditLogListener an AuditLogListener instance to notify when details are audited, or null.
     * @param auditDetailFilter an AuditDetailFilter that can be used to prevent some details from being audited, or null.
     * @param eventPub  an ApplicationEventPublisher that will be used as the sink for audit detail events.  Required.
     * @param logger  logger to which details will be written.
     */
    public Auditor(Object source, AuditLogListener auditLogListener, AuditDetailFilter auditDetailFilter, ApplicationEventPublisher eventPub, Logger logger) {
        if(source  == null) throw new IllegalArgumentException("Event source is NULL. Cannot add AuditDetail to the audit record.");
        this.source = source;
        this.eventPub = eventPub;
        this.logger = logger;
        this.listener = auditLogListener;
        this.filter = auditDetailFilter;
    }

    public void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e) {
        if ( eventPub != null && (filter == null || filter.isAuditable(msg)) )
            eventPub.publishEvent(new AuditDetailEvent(source, new AuditDetail(msg, params == null ? null : params, e), e));

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
