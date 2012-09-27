package com.l7tech.external.assertions.ahttp.server;

import com.l7tech.external.assertions.ahttp.SubmitAsyncHttpResponseAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;
import java.util.Map;

/**
 * Server side implementation of the SubmitAsyncHttpResponseAssertion.
 *
 * @see com.l7tech.external.assertions.ahttp.SubmitAsyncHttpResponseAssertion
 */
public class ServerSubmitAsyncHttpResponseAssertion extends AbstractMessageTargetableServerAssertion<SubmitAsyncHttpResponseAssertion> {

    final String requestIdTemplate;
    final String[] varsUsed;

    public ServerSubmitAsyncHttpResponseAssertion(final SubmitAsyncHttpResponseAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        this.requestIdTemplate = assertion.getCorrelationId();
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        Map<String,?> varMap = context.getVariableMap(varsUsed, getAudit());
        String requestId = ExpandVariables.process(requestIdTemplate, varMap, getAudit());

        boolean success = AsyncHttpTransportModule.sendResponseToPendingRequest(requestId, message, false);
        return success ? AssertionStatus.NONE : AssertionStatus.FAILED;
    }
}
