package com.l7tech.external.assertions.concall.server;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;

import java.io.IOException;

/**
 *
 */
public class ServerDelayedCopyVariableAssertion extends AbstractServerAssertion<DelayedCopyVariableAssertion> {
    public ServerDelayedCopyVariableAssertion(DelayedCopyVariableAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Thread.sleep(assertion.getDelayMillis());
        } catch (InterruptedException e) {
            throw new AssertionStatusException(e);
        }

        try {
            Object value = context.getVariable(assertion.getSourceVariableName());
            context.setVariable(assertion.getDestVariableName(), copyValue(value));
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(e);
        }

        return AssertionStatus.NONE;
    }

    private Object copyValue(Object value) throws IOException {
        if (value == null)
            throw new AssertionStatusException("Value is null");
        if (value instanceof String) {
            // No need to copy it
            return value;
        } else if (value instanceof Message) {
            return ServerConcurrentAllAssertion.cloneMessageBody((Message)value);
        } else
            throw new AssertionStatusException("Value must be either a String or a Message; actual value type: " + value.getClass());
    }
}
