/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Interface that connects into the <code>TestMessageProcessor</code>.
 * Fopr testing purposes only.
 * <p/>
 * It allows implementationsd to access the PolicyEnforcementContext.
 *
 * @author emil
 * @version Apr 8, 2005
 */
public interface MessageProcessorListener {
    /**
     * Invoked before processing the message by MessageProcessor
     *
     * @param context the policy enforcement context
     */
    void beforeProcessMessage(PolicyEnforcementContext context);

    /**
     * Invoked after processing the message by MessageProcessor
     *
     * @param context the policy enforcement context
     */
    void afterProcessMessage(PolicyEnforcementContext context);

}