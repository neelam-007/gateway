/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.message.PolicyApplicationContext;

import java.io.IOException;

/**
 * PolicyManager for testing.  Provides a fixed policy that is manually set.
 * User: mike
 * Date: Jun 17, 2003
 * Time: 2:50:49 PM
 */
public class PolicyManagerStub implements PolicyManager {
    private Policy policy;

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Policy getPolicy() {
        return policy;
    }

    public Policy getPolicy(PolicyApplicationContext request) {
        return policy;
    }

    public void flushPolicy(PolicyApplicationContext request) {
    }

    public void updatePolicy(PolicyApplicationContext request, String serviceId) throws ConfigurationException, IOException {
    }
}
