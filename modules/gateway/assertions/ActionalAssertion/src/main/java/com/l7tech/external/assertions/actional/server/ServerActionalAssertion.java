package com.l7tech.external.assertions.actional.server;

import com.l7tech.external.assertions.actional.ActionalAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Server side implementation of the ActionalAssertion.
 *
 * @see com.l7tech.external.assertions.actional.ActionalAssertion
 */
public class ServerActionalAssertion extends AbstractServerAssertion<ActionalAssertion> {

    public ServerActionalAssertion(ActionalAssertion assertion) throws PolicyAssertionException {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
       InterceptorEventListener interceptor = InterceptorEventListener.getInstance();
        if (interceptor == null) {
            context.setVariable(ActionalAssertion.INTERCEPTOR_ENABLE_CLUSTER_PROPERTY, Boolean.FALSE);
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Interceptor event listener could not be instantiated.");
            return AssertionStatus.SERVER_ERROR;
        }

        boolean enabled = interceptor.isEnabled();
        context.setVariable(ActionalAssertion.INTERCEPTOR_ENABLE_CLUSTER_PROPERTY, enabled);
        return enabled ? AssertionStatus.NONE : AssertionStatus.FAILED;
    }
}
