/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.message;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.ProcessingContext;

import java.util.logging.Logger;

/**
 * Holds message processing state needed by policy application point (SSB) message processor and policy assertions.
 */
public class PolicyApplicationContext extends ProcessingContext {
    private static final Logger logger = Logger.getLogger(PolicyApplicationContext.class.getName());

    public PolicyApplicationContext(Message request, Message response) {
        super(request, response);
    }
}
