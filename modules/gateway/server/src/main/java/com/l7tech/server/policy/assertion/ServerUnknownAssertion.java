package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * The <code>ServerUnknownAssertion</code> is an assertion that indicates
 * that there is an unknown assertion in the policy tree. This assertion
 * always return a negative result.
 * <p/>
 * One known scenario for unknown assertion is where, after the custom assertion
 * deinstall, there are remaining policies with custom assertions. We introduce
 * unknown assertion that always returns the {@link AssertionStatus#FALSIFIED}, and
 * logs the warning message.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServerUnknownAssertion extends AbstractServerAssertion<UnknownAssertion> implements ServerAssertion<UnknownAssertion> {

    public ServerUnknownAssertion(UnknownAssertion a) {
        super(a);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        final boolean hasDetailMessage = assertion.getDetailMessage() != null;
        String desc = hasDetailMessage ? assertion.getDetailMessage() : "No more description available";

        logAndAudit( AssertionMessages.UNKNOWN_ASSERTION, desc );
        return AssertionStatus.FALSIFIED;
    }
}
