/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.PolicyLockedException;

import java.util.Collections;
import java.util.Set;

/**
 * A policy manager that "manages" a single statically-configured policy, and declines to store any new policies.
 */
public class StaticPolicyManager implements PolicyManager {
    private final Policy policy;

    /**
     * Create a new StaticPolicyManager that will recommend using the specified policy for all requests.
     * @param policy the policy to use.  Must not be null.
     */
    public StaticPolicyManager(Policy policy) {
        if (policy == null) throw new NullPointerException();
        this.policy = policy;
    }

    public void flushPolicy(PolicyAttachmentKey policyAttachmentKey) {
    }

    public Policy getPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return policy;
    }

    public Policy findMatchingPolicy(PolicyAttachmentKey policyAttachmentKey) {
        return policy;
    }

    public void setPolicy(PolicyAttachmentKey key, Policy policy) throws PolicyLockedException {
        throw new PolicyLockedException("Unable to save new policy: this Bridge instance is using a single statically-configured policy");
    }

    public Set getPolicyAttachmentKeys() {
        return Collections.EMPTY_SET;
    }

    public void clearPolicies() {
    }
}
