/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.JmsRoutingAssertion;

/**
 *
 * @author mike
 * @version 1.0
 */
public class ClientJmsRoutingAssertion extends ClientRoutingAssertion {
    public ClientJmsRoutingAssertion(JmsRoutingAssertion data) {
        super(data);
    }
}
