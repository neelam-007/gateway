package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.util.EventChannel;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * This listener manages AuditDetailEvent and message processing specific events.
 *
 * When an {@link AuditDetailEvent} is received via some code calling {@link Auditor}.logAndAudit, a detail will be
 * added to the AuditContext which will be associated with the current thread. It is up to the users of
 * Audit.logAndAudit to ensure the audit context is flushed. This happens automatically for message processing via the
 * PolicyEnforcementContext being closed, via the MessageProcessor, however for sub systems using Auditor, they will
 * need to ensure flush is called. An easy way to achieve this is to publish a SystemEvent.
 *
 * @author alex
 */
public class MessageProcessingAuditListener implements ApplicationListener {
    private final MessageSummaryAuditFactory messageSummaryAuditFactory;
    private final AuditContext auditContext;

    public MessageProcessingAuditListener(MessageSummaryAuditFactory msaf, AuditContext auditContext, EventChannel messageProcessingEventChannel) {
        this.messageSummaryAuditFactory = msaf;
        this.auditContext = auditContext;
        messageProcessingEventChannel.addApplicationListener(this);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        Object source = event.getSource();
        if (event instanceof AuditDetailEvent) {
            AuditDetailEvent auditDetailEvent = (AuditDetailEvent)event;
            //noinspection ThrowableResultOfMethodCallIgnored
            auditContext.addDetail(auditDetailEvent.getDetailWithInfo());
        } else if (event instanceof MessageProcessed) {
            MessageProcessed mp = (MessageProcessed)event;
            auditContext.setCurrentRecord(messageSummaryAuditFactory.makeEvent(mp.getContext(), mp.getStatus()));
        } else if (event instanceof FaultProcessed) {
            FaultProcessed fp = (FaultProcessed)event;
            AuditDetail detail = messageSummaryAuditFactory.makeEvent(fp.getContext(), fp.getFaultMessage());
            if (detail!=null) auditContext.addDetail(detail, source);
        }
    }
}
