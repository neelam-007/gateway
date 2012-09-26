package com.l7tech.external.assertions.policybundleinstaller;

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
     * @throws PolicyAssertionException
     * @throws IOException
     */
    AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException;
}
