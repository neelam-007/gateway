package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.InvokePolicyAsyncAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

public class ServerInvokePolicyAsyncAssertion extends AbstractServerAssertion<InvokePolicyAsyncAssertion> {

    public ServerInvokePolicyAsyncAssertion(final InvokePolicyAsyncAssertion assertion) {
        super(assertion);

    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return null;
    }
}
