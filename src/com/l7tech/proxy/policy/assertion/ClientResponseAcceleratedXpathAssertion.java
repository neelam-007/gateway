/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.ResponseXpathAssertion;


/**
 * Client side implementation of accelerated response xpath assertion.
 * The "Accelerated" request and response xpath assertions are not "real" policy assertions; they are just
 * alternate implementations of the request and response xpath assertions that use the hardware instead.  The
 * ClientPolicyFactory instantiates the hardware-assisted versions if hardware support seems to be available.
 */
public class ClientResponseAcceleratedXpathAssertion extends ClientAcceleratedXpathAssertion {
    public ClientResponseAcceleratedXpathAssertion(ResponseXpathAssertion assertion) {
        super(assertion, false, new ClientResponseXpathAssertion(assertion));
    }
}
