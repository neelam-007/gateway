package com.l7tech.external.assertions.actional.server;

import com.l7tech.external.assertions.actional.ActionalAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the ActionalAssertion.
 *
 * @see com.l7tech.external.assertions.actional.ActionalAssertion
 */
public class ServerActionalAssertion extends AbstractServerAssertion<ActionalAssertion> {
    private static final Logger logger = Logger.getLogger(ServerActionalAssertion.class.getName());

    private final Auditor auditor;

    public ServerActionalAssertion(ActionalAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
       InterceptorEventListener interceptor = InterceptorEventListener.getInstance();
        if (interceptor == null) {
            context.setVariable(ActionalAssertion.INTERCEPTOR_ENABLE_CLUSTER_PROPERTY, Boolean.FALSE);
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Interceptor event listener could not be instantiated.");
            return AssertionStatus.SERVER_ERROR;
        }

        boolean enabled = interceptor.isEnabled();
        context.setVariable(ActionalAssertion.INTERCEPTOR_ENABLE_CLUSTER_PROPERTY, enabled);
        return enabled ? AssertionStatus.NONE : AssertionStatus.FAILED;
    }
}
