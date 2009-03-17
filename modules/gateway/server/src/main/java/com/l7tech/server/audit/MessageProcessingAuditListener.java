/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.event.MessageProcessed;
import com.l7tech.server.util.EventChannel;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessingAuditListener implements ApplicationListener {
    private final MessageSummaryAuditFactory messageSummaryAuditFactory;
    private final AuditContext auditContext;

    public MessageProcessingAuditListener(MessageSummaryAuditFactory msaf, AuditContext auditContext, EventChannel messageProcessingEventChannel) {
        this.messageSummaryAuditFactory = msaf;
        this.auditContext = auditContext;
        messageProcessingEventChannel.addApplicationListener(this);
    }

    public void onApplicationEvent(ApplicationEvent event) {
        Object source = event.getSource();
        if (event instanceof AuditDetailEvent) {
            AuditDetailEvent auditDetailEvent = (AuditDetailEvent)event;
            auditContext.addDetail(auditDetailEvent.getDetail(), source, auditDetailEvent.getException());
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
