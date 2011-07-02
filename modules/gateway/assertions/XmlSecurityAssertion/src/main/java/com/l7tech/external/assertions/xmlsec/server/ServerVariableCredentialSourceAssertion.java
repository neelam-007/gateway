package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.VariableCredentialSourceAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;

/**
 *
 */
public class ServerVariableCredentialSourceAssertion extends AbstractMessageTargetableServerAssertion<VariableCredentialSourceAssertion> {
    private final String variableName;

    public ServerVariableCredentialSourceAssertion(VariableCredentialSourceAssertion assertion) throws PolicyAssertionException {
        super(assertion, assertion);
        variableName = assertion.getVariableName();
        if (variableName == null || variableName.length() < 1)
            throw new PolicyAssertionException(assertion, "Variable name is not set");
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws IOException, PolicyAssertionException
    {
        try {
            Object value = context.getVariable(variableName);
            if (value == null) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Value is null for variable: " + variableName);
                return AssertionStatus.SERVER_ERROR;
            }

            NonSoapSecurityServerUtil.addObjectAsCredentials(authContext, value, assertion.getClass());
            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
            return AssertionStatus.SERVER_ERROR;
        } catch (UnsupportedTokenTypeException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Type not supported for variable credentials for variable " + variableName + ": " + ExceptionUtils.getMessage(e));
            return AssertionStatus.SERVER_ERROR;
        }
    }

}
