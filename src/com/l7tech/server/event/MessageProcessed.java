/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationEvent;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessed extends ApplicationEvent {
    private final AssertionStatus status;
    private final PolicyEnforcementContext context;

    public MessageProcessed(PolicyEnforcementContext context, AssertionStatus status, MessageProcessor messageProcessor) {
        super(messageProcessor);
        this.context = context;
        this.status = status;
    }

    public AssertionStatus getStatus() {
        return status;
    }

    public PolicyEnforcementContext getContext() {
        return context;
    }
}
