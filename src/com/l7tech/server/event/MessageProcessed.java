/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.objectmodel.event.Event;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessed extends Event {
    public MessageProcessed(Request request, Response response, AssertionStatus status) {
        super(MessageProcessor.getInstance());
        this.request = request;
        this.response = response;
        this.status = status;
    }

    public void sendTo(EventListener listener) {
        if (listener instanceof MessageProcessingEventListener)
            ((MessageProcessingEventListener)listener).messageProcessed(request, response, status);
        else
            super.sendTo(listener);
    }

    private Request request;
    private Response response;
    private AssertionStatus status;
}
