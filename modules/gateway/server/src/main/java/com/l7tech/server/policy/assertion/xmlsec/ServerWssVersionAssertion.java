package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssVersionAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Server side implementation of WssVersionAssertion.
 */
public class ServerWssVersionAssertion extends AbstractServerAssertion<WssVersionAssertion> {

    public ServerWssVersionAssertion( final WssVersionAssertion assertion ) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        context.setResponseWss11(true);
        context.getRequest().getSecurityKnob().setNeedsSignatureConfirmations(true);
        return AssertionStatus.NONE;
    }
}
