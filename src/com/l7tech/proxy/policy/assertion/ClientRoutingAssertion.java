/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.Assertion;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientRoutingAssertion extends UnimplementedClientAssertion {
    public ClientRoutingAssertion() {
    }

    public ClientRoutingAssertion(Assertion source) {
        super(source);
    }
}
