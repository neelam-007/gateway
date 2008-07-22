/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import org.springframework.context.ApplicationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Instances are created and fired by {@link com.l7tech.server.MessageProcessor} and/or
 * {@link com.l7tech.server.policy.assertion.ServerAssertion}s to notify components of the status
 * of message processing.
 * <p/>
 * In order to receive events, components must register as a Spring bean (e.g. in <code>webApplicationContext.xml</code>)
 * and implement {@link org.springframework.context.ApplicationListener}.
 */
public abstract class MessageProcessingEvent extends ApplicationEvent {
    protected final PolicyEnforcementContext context;

    public MessageProcessingEvent(Object object, PolicyEnforcementContext context) {
        super(object);
        this.context = context;
    }

    /**
     * @return the PolicyEnforcementContext for the message being processed
     */
    public PolicyEnforcementContext getContext() {
        return context;
    }
}
