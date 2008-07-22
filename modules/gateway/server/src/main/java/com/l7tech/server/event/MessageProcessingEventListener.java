/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public interface MessageProcessingEventListener extends EventListener {
    void messageProcessed(PolicyEnforcementContext context, AssertionStatus status);
}
