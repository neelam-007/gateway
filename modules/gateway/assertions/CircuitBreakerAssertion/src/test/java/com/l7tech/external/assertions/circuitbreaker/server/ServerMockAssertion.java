package com.l7tech.external.assertions.circuitbreaker.server;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class ServerMockAssertion extends AbstractServerAssertion<MockAssertion> {

    public ServerMockAssertion(@NotNull MockAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return assertion.getReturnStatus();
    }
}
