/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.ResponseXpathAssertion;
import org.springframework.context.ApplicationContext;

/**
 * A hardware-accelerated version of {@link ServerResponseXpathAssertion}.
 * The "Accelerated" request and response xpath assertions are not "real" policy assertions; they are just
 * alternate implementations of the request and response xpath assertions that use the hardware instead.  The
 * ServerPolicyFactory instantiates the hardware-assisted versions if hardware support seems to be available.
 */
public class ServerResponseAcceleratedXpathAssertion extends ServerAcceleratedXpathAssertion {
    public ServerResponseAcceleratedXpathAssertion(ResponseXpathAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext, new ServerResponseXpathAssertion(assertion, applicationContext));
    }
}
