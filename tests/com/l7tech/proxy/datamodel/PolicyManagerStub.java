/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;

import java.io.IOException;

/**
 * PolicyManager for testing.  Provides a fixed policy that is manually set.
 * User: mike
 * Date: Jun 17, 2003
 * Time: 2:50:49 PM
 */
public class PolicyManagerStub implements PolicyManager {
    private Assertion policy;

    public void setPolicy(Assertion policy) {
        this.policy = policy;
    }

    public Assertion getPolicy() {
        return policy;
    }

    public Assertion getPolicy(PendingRequest request) {
        return policy;
    }
}
