/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditDetailEvent;
import com.l7tech.server.event.MessageProcessed;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessingAuditListener implements ApplicationListener {
    private final MessageSummaryAuditFactory messageSummaryAuditFactory;
    private final AuditContext auditContext;

    public MessageProcessingAuditListener(MessageSummaryAuditFactory msaf, AuditContext auditContext) {
        this.messageSummaryAuditFactory = msaf;
        this.auditContext = auditContext;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AuditDetailEvent) {
            AuditDetailEvent auditDetailEvent = (AuditDetailEvent)event;
            auditContext.addDetail(auditDetailEvent.getDetail());
        } else if (event instanceof MessageProcessed) {
            MessageProcessed mp = (MessageProcessed)event;
            auditContext.setCurrentRecord(messageSummaryAuditFactory.makeEvent(mp.getContext(), mp.getStatus()));
        }
    }
}
