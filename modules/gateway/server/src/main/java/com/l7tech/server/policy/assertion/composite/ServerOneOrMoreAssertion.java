package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;

public final class ServerOneOrMoreAssertion extends ServerCompositeAssertion<OneOrMoreAssertion> {

    private final AssertionResultListener assertionResultListener = new AssertionResultListener() {
        @Override
        public boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result) {
            if (result == AssertionStatus.NONE) {
                return false;
            } else {
                seenAssertionStatus(context, result);
                return true;
            }
        }
    };

    public ServerOneOrMoreAssertion( OneOrMoreAssertion data, ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(data, applicationContext);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final List<ServerAssertion> kids = getChildren();
        AssertionStatus result = AssertionStatus.FALSIFIED;

        if (kids.size() > 0) {
            result = iterateChildren(context, assertionResultListener);
        }

        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(context);

        return result;
    }
}
