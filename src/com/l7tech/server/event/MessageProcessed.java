/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessed extends Event {
    private final AssertionStatus status;
    private final PolicyEnforcementContext context;

    public MessageProcessed(PolicyEnforcementContext context, AssertionStatus status) {
        super(MessageProcessor.getInstance());
        this.context = context;
        this.status = status;
    }

    public void sendTo(EventListener listener) {
        if (listener instanceof MessageProcessingEventListener)
            ((MessageProcessingEventListener)listener).messageProcessed(context, status);
        else
            super.sendTo(listener);
    }
}
