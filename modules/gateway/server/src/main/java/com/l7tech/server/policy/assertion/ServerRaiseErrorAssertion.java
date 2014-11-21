package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RaiseErrorAssertion;
import com.l7tech.policy.assertion.RaisedByPolicyException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * The server side implementation of the {@link com.l7tech.policy.assertion.RaiseErrorAssertion}.
 */
public class ServerRaiseErrorAssertion extends AbstractServerAssertion<RaiseErrorAssertion> {

    public ServerRaiseErrorAssertion(@NotNull RaiseErrorAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        throw new RaisedByPolicyException(assertion, "RaiseErrorAssertion is stopping execution.");
    }
}
