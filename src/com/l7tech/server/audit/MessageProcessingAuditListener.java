/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.MessageProcessingEventListener;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessingAuditListener implements MessageProcessingEventListener {
    private final ApplicationContext applicationContext;
    private final MessageSummaryAuditFactory messageSummaryAuditFactory;

    public MessageProcessingAuditListener(ApplicationContext appCtx) {
        applicationContext = appCtx;
        if (applicationContext == null) {
            throw new IllegalArgumentException("Application Context is required");
        }
        messageSummaryAuditFactory = (MessageSummaryAuditFactory)applicationContext.getBean("messageSummaryAuditFactory");
    }

    public void messageProcessed( PolicyEnforcementContext context, AssertionStatus status ) {
        AuditContext.getCurrent(applicationContext).setCurrentRecord(messageSummaryAuditFactory.makeEvent(context, status));
    }
}
