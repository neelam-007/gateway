package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;

import java.util.logging.Logger;

public class Auditor extends LoggingAudit {
    private final ApplicationEventPublisher eventPub;
    private final Object source;
    private final AuditLogListener listener;
    private final AuditDetailFilter filter;
    private final AuditLogFormatter formatter;

    /**
     * Create an Auditor that will only log and which has no message source.
     *
     * @param logger  logger to which details will be written.
     */
    protected Auditor(Logger logger) {
        super(logger);
        this.source = null;
        this.eventPub = null;
        this.listener = null;
        this.filter = null;
        this.formatter = new AuditLogFormatter();
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
     * @param eventPub  an ApplicationEventPublisher that will be used as the sink for audit detail events, or null
     *                    to act as a logging-only auditor.
     * @param logger  logger to which details will be written.
     */
    public Auditor(Object source, BeanFactory beanFactory, ApplicationEventPublisher eventPub, Logger logger) {
        this(source, findListener(beanFactory), findFilter(beanFactory), eventPub, logger);
    }

    private static AuditLogListener findListener(BeanFactory beanFactory) {
        if (beanFactory == null)
            return null;
        return beanFactory.containsBean("auditLogListener") ? beanFactory.getBean("auditLogListener", AuditLogListener.class) : null;
    }

    private static AuditDetailFilter findFilter(BeanFactory beanFactory) {
        if (beanFactory == null)
            return null;
        return beanFactory.containsBean("auditDetailFilter") ? beanFactory.getBean("auditDetailFilter", AuditDetailFilter.class) : null;
    }

    /**
     * Create an Auditor that will send details to the audit context and to the specified Logger.
     *
     * @param source  source object for audit events.  Required.
     * @param auditLogListener an AuditLogListener instance to notify when details are audited, or null.
     * @param auditDetailFilter an AuditDetailFilter that can be used to prevent some details from being audited, or null.
     * @param eventPub  an ApplicationEventPublisher that will be used as the sink for audit detail events, or null
     *                  to act as a log-only auditor.
     * @param logger  logger to which details will be written.
     */
    public Auditor(Object source, AuditLogListener auditLogListener, AuditDetailFilter auditDetailFilter, ApplicationEventPublisher eventPub, Logger logger) {
        super(logger);
        if(source  == null) throw new IllegalArgumentException("Event source is NULL. Cannot add AuditDetail to the audit record.");
        this.source = source;
        this.eventPub = eventPub;
        this.listener = auditLogListener;
        this.filter = auditDetailFilter;
        this.formatter = new AuditLogFormatter();
    }

    @Override
    public void logAndAudit(AuditDetailMessage msg, String[] params, Throwable e) {
        String loggerName = logger == null ? null : logger.getName();
        if ( eventPub != null && (filter == null || filter.isAuditable(msg)) )
            eventPub.publishEvent(new AuditDetailEvent(source, new AuditDetail(msg, params == null ? null : params, e), e, loggerName));

        if (logger == null) return;

        log( msg, params, e );
    }

    @Override
    public void logAndAudit(AuditDetailMessage msg, String... params) {
        logAndAudit(msg, params, null);
    }

    @Override
    public void logAndAudit(AuditDetailMessage msg) {
        logAndAudit(msg, null, null);
    }

    @Override
    protected void logDetail( final AuditDetailMessage msg, final String[] params, final Throwable e ) {
        if ( listener != null ) {
            listener.notifyDetailCreated( logger.getName(), logger.getName(), msg, params, formatter, e );
        } else {
            super.logDetail( msg, params, e );
        }
    }

    @Override
    protected String formatDetail( final AuditDetailMessage msg ) {
        return formatter.formatDetail(msg);
    }

    /**
     *
     */
    public static final class DefaultAuditorFactory implements ApplicationContextAware, AuditFactory {

        //- PUBLIC

        @Override
        public Auditor newInstance( final Object source, final Logger logger ) {
            if ( eventPublisher == null ) throw new IllegalStateException("Not initialized");
            return new Auditor( source, auditLogListener, auditDetailFilter, eventPublisher, logger );
        }

        @Override
        public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
            eventPublisher = applicationContext;
        }

        //- PACKAGE

        DefaultAuditorFactory( final AuditDetailFilter auditDetailFilter,
                               final AuditLogListener auditLogListener ) {
            this.auditDetailFilter = auditDetailFilter;
            this.auditLogListener = auditLogListener;
        }

        //- PRIVATE

        private final AuditDetailFilter auditDetailFilter;
        private final AuditLogListener auditLogListener;
        private ApplicationEventPublisher eventPublisher;
    }

}
