/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.HttpRoutingAssertion;

/**
 *
 * @author mike
 * @version 1.0
 */
public class ClientHttpRoutingAssertion extends ClientRoutingAssertion {
    public ClientHttpRoutingAssertion(HttpRoutingAssertion data) {
        super(data);
    }
}
