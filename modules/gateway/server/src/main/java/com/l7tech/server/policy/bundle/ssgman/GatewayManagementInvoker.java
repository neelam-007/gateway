package com.l7tech.server.policy.bundle.ssgman;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

public interface GatewayManagementInvoker {

    /**
     * Invoke the Gateway Management Server assertion. This is intended only for use with installing bundles.
     *
     * @param context PEC
     * @return assertion status
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     * @throws java.io.IOException
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException;
}
