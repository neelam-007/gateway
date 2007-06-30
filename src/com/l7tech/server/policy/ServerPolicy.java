/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.AbstractReferenceCounted;

import java.io.IOException;

/**
 * Ensures that {@link ServerAssertion#close()} can be called safely when no more traffic will arrive by
 * giving out handles that maintain a reference count, and only
 * closing the policy only when the reference count hits zero.
 */
public class ServerPolicy extends AbstractReferenceCounted<ServerPolicyHandle> {
    private final ServerAssertion rootAssertion;

    public ServerPolicy(ServerAssertion rootAssertion) {
        this.rootAssertion = rootAssertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        return rootAssertion.checkRequest(context);
    }

    protected ServerPolicyHandle createHandle() {
        return new ServerPolicyHandle(this);
    }

    /**
     * Closes this policy.  May block until all message traffic passing through this policy has concluded.
     */
    protected void doClose() {
        rootAssertion.close();
    }
}
