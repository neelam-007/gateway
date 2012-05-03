package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public final class ServerExactlyOneAssertion extends ServerCompositeAssertion<ExactlyOneAssertion> {
    public ServerExactlyOneAssertion( ExactlyOneAssertion data, ApplicationContext applicationContext ) throws PolicyAssertionException, LicenseException {
        super( data, applicationContext );
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus result;
        final int[] numSucceeded = {0};
        result = iterateChildren(context, new AssertionResultListener() {
            @Override
            public boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result) {
                if (result == AssertionStatus.NONE) {
                    ++numSucceeded[0];
                }
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        result = numSucceeded[0] == 1 ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;

        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(context);

        return result;
    }
}
