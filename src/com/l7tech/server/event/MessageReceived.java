/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Should be fired after the service is resolved, but before policy begins execution
 */
public class MessageReceived extends MessageProcessingEvent {
    public MessageReceived(Object o, PolicyEnforcementContext context) {
        super(o, context);
    }
}
