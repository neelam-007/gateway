/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.ResponseAcceleratedXpathAssertion;

/**
 * Client side implementation of accelerated response xpath assertion.
 */
public class ClientResponseAcceleratedXpathAssertion extends ClientAcceleratedXpathAssertion {
    public ClientResponseAcceleratedXpathAssertion(ResponseAcceleratedXpathAssertion assertion) {
        super(assertion, false, new ClientResponseXpathAssertion(assertion));
    }
}
