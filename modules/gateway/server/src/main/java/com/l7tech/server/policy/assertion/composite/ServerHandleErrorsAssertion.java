package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.HandleErrorsAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;


public class ServerHandleErrorsAssertion extends ServerCompositeAssertion<HandleErrorsAssertion> {
    private static final Logger logger = Logger.getLogger(ServerHandleErrorsAssertion.class.getName());

    private final AssertionResultListener assertionResultListener = new AssertionResultListener() {
        @Override
        public boolean assertionFinished(final PolicyEnforcementContext context, final AssertionStatus result) {
            return AssertionStatus.NONE.equals(result);
        }
    };

    public ServerHandleErrorsAssertion(final HandleErrorsAssertion data, final ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(data, applicationContext);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) {
        AssertionStatus status = AssertionStatus.FALSIFIED;
        try {
            status = iterateChildren(context, assertionResultListener);
        } catch (Exception e) {
            context.setVariable(assertion.getVariablePrefix() + ".message", e.getLocalizedMessage());
        }
        return status;
    }
}
