package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * @author alex
 */
public class ServerFalseAssertion<AT extends Assertion> extends AbstractServerAssertion<AT> {
    public ServerFalseAssertion( AT ass ) {
        super(ass);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.FALSIFIED;
    }
}
