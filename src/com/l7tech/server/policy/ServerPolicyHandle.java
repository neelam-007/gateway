/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy;

import com.l7tech.server.util.Handle;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.IOException;

/**
 * Handle pointing at a ServerPolicy instance.
 */
public class ServerPolicyHandle extends Handle<ServerPolicy> {
    protected ServerPolicyHandle(ServerPolicy cs) {
        super(cs);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
        ServerPolicy target = getTarget();
        if (target == null) throw new IllegalStateException("ServerPolicyHandle has already been closed");
        return target.checkRequest(context);
    }

    /** Open up access so service cache can use this.  ONLY ServiceCache should use this. */
    public ServerPolicy getTarget() {
        return super.getTarget();
    }
}
