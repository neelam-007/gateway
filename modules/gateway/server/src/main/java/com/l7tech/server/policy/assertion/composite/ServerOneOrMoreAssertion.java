package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.gateway.common.LicenseException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;

public final class ServerOneOrMoreAssertion extends ServerCompositeAssertion<OneOrMoreAssertion> {
    public ServerOneOrMoreAssertion( OneOrMoreAssertion data, ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(data, applicationContext);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final List<ServerAssertion> kids = getChildren();
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for (ServerAssertion kid : kids) {

            context.assertionStarting(kid);

            try {
                result = kid.checkRequest(context);
            } catch (AssertionStatusException e) {
                result = e.getAssertionStatus();
            }

            context.assertionFinished(kid, result);

            if (result == AssertionStatus.NONE)
                return result;

            seenAssertionStatus(context, result);
        }

        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(context);

        return result;
    }
}
