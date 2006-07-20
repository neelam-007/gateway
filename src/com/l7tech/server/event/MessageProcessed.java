/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Should be fired after policy execution concludes.
 */
public class MessageProcessed extends MessageProcessingEvent {
    private final AssertionStatus status;

    public MessageProcessed(PolicyEnforcementContext context, AssertionStatus status, MessageProcessor messageProcessor) {
        super(messageProcessor, context);
        this.status = status;
    }

    public AssertionStatus getStatus() {
        return status;
    }
}
