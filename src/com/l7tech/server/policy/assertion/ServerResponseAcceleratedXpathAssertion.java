/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.ResponseAcceleratedXpathAssertion;
import org.springframework.context.ApplicationContext;

/**
 * A hardware-accelerated version of {@link ServerResponseXpathAssertion}.
 */
public class ServerResponseAcceleratedXpathAssertion extends ServerAcceleratedXpathAssertion {
    public ServerResponseAcceleratedXpathAssertion(ResponseAcceleratedXpathAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext, new ServerResponseXpathAssertion(assertion));
    }
}
