/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.ClientPolicyFactory;

/**
 * Holds a policy assertion tree along with versioning metadata.
 */
public class Policy {
    private final String version;
    private final Assertion assertion;
    private ClientAssertion clientAssertion;
    private boolean valid = true;

    public Policy(Assertion assertion, String version) {
        this.assertion = assertion;
        this.version = version;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public synchronized ClientAssertion getClientAssertion() {
        if (clientAssertion != null)
            return clientAssertion;
        if (assertion != null)
            return clientAssertion = ClientPolicyFactory.getInstance().makeClientPolicy(assertion);
        return null;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Check if this policy has ever caused a PolicyAssertionException.  If it has, you are better off
     * ignoring it when processing messages in the hope that the SSG will send you a new one.
     *
     * @return false if this policy has ever caused a PolicyAssertionException; otherwise true.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Note that this policy is known to have caused at least one PolicyAssertionException
     * and so should be ignored when processing messages.
     */
    public void invalidate() {
        valid = false;
    }
}
