package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public final class ServerAllAssertion extends ServerCompositeAssertion<AllAssertion> {

    private final AssertionResultListener assertionResultListener = new AssertionResultListener() {
        @Override
        public boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result) {
            if (result != AssertionStatus.NONE) {
                seenAssertionStatus(context, result);
                rollbackDeferredAssertions(context);
                return false;
            }
            return true;
        }
    };

    public ServerAllAssertion(AllAssertion data, ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(data, applicationContext);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return iterateChildren(context, assertionResultListener);
    }

}
