/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.message.PolicyApplicationContext;

import java.io.IOException;
import java.util.Set;
import java.util.Collections;

/**
 * PolicyManager for testing.  Provides a fixed policy that is manually set.
 * User: mike
 * Date: Jun 17, 2003
 * Time: 2:50:49 PM
 */
public class PolicyManagerStub extends LocalPolicyManager {
    private Policy policy;

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public synchronized void setPolicy(PolicyAttachmentKey key, Policy policy) {
        // ignore
    }

    public synchronized void clearPolicies() {
        // ignore
    }

    public void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        // ignore
    }

    public Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) throws IOException {
        return policy;
    }

    public Set getPolicyAttachmentKeys() {
        return Collections.EMPTY_SET;
    }
}
