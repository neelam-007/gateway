/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.MessageProcessingEventListener;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessingAuditListener implements MessageProcessingEventListener {
    public void messageProcessed( PolicyEnforcementContext context, AssertionStatus status ) {
        AuditContext.getCurrent().add(MessageSummaryAuditFactory.makeEvent(context, status));
    }
}
