/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

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

    public synchronized void setPolicy(PolicyAttachmentKey key, Policy policy) {
        // ignore
    }

    public synchronized void clearPolicies() {
        // ignore
    }

    public PolicyManager getDelegate() {
        return null;
    }

    public void setDelegate(PolicyManager delegate) {

    }

    public HashMap getPolicyMap() {
        return null;
    }

    public void setPolicyMap(HashMap policyMap) {

    }

    public void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
        // ignore
    }

    public Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return policy;
    }

    public Policy findMatchingPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return policy;
    }

    public Set getPolicyAttachmentKeys() {
        return Collections.EMPTY_SET;
    }
}
