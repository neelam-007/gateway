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
 * User: mike
 * Date: Sep 2, 2003
 * Time: 4:47:28 PM
 */
public class Policy {
    private final String version;
    private final Assertion assertion;
    private ClientAssertion clientAssertion;

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
}
