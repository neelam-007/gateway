package com.l7tech.server.policy.assertion.composite;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.HandleErrorsAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;


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
            context.setVariable(assertion.getVariablePrefix() + ".message", ExceptionUtils.getMessage(e));
            logAndAudit(AssertionMessages.HANDLE_ERRORS_MSG, ExceptionUtils.getMessage(e));
        } catch (IOException e) {
            if (assertion.isIncludeIOException()) {
                context.setVariable(assertion.getVariablePrefix() + ".message", ExceptionUtils.getMessage(e));
                logAndAudit(AssertionMessages.HANDLE_ERRORS_MSG, ExceptionUtils.getMessage(e));
            } else {
                throw e;
            }
        }
        return status;
    }
}
