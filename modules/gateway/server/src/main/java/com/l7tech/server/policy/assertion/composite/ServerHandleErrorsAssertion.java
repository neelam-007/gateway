package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.HandleErrorsAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;


public class ServerHandleErrorsAssertion extends ServerCompositeAssertion<HandleErrorsAssertion> {

    private final AssertionResultListener assertionResultListener = new AssertionResultListener() {
        @Override
        public boolean assertionFinished(PolicyEnforcementContext context, AssertionStatus result) {
            if (!AssertionStatus.NONE.equals(result)) {
                seenAssertionStatus(context, result);
                rollbackDeferredAssertions(context);
                return false;
            }
            return true;
        }
    };

    public ServerHandleErrorsAssertion(final HandleErrorsAssertion data, final ApplicationContext applicationContext) throws PolicyAssertionException, LicenseException {
        super(data, applicationContext);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException {
        AssertionStatus status = AssertionStatus.FAILED;
        try {
            status = iterateChildren(context, assertionResultListener);
        } catch (PolicyAssertionException e) {
            context.setVariable(assertion.getVariablePrefix() + ".message", e.getLocalizedMessage());
        } catch(IOException e){
            if(assertion.isIncludeIOException()){
                context.setVariable(assertion.getVariablePrefix() + ".message", e.getLocalizedMessage());
            }
            else {
                throw e;
            }
        }
        return status;
    }
}
