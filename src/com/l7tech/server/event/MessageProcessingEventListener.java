/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;

import java.util.EventListener;

/**
 * @author alex
 * @version $Revision$
 */
public interface MessageProcessingEventListener extends EventListener {
    void messageProcessed(Request request, Response response, AssertionStatus status);
}
