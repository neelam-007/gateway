/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.RequestAcceleratedXpathAssertion;

/**
 * Hardware-accelerated version of client request xpath assertion.
 */
public class ClientRequestAcceleratedXpathAssertion extends ClientAcceleratedXpathAssertion {
    public ClientRequestAcceleratedXpathAssertion(RequestAcceleratedXpathAssertion assertion) {
        super(assertion, true, new ClientRequestXpathAssertion(assertion));
    }
}
