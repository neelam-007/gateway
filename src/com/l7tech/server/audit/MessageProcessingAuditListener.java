/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.MessageProcessingEventListener;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessingAuditListener implements MessageProcessingEventListener {
    public void messageProcessed( Request request, Response response, AssertionStatus status ) {
        AuditContext.getCurrent().add(MessageSummaryAuditFactory.makeEvent(request.getAuditLevel(), status));
    }
}
