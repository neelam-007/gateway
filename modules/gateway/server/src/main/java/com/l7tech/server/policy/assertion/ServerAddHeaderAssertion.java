package com.l7tech.server.policy.assertion;

import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

/**
 * Add a header to a message.
 */
public class ServerAddHeaderAssertion extends AbstractMessageTargetableServerAssertion<AddHeaderAssertion> {
    private final String[] variablesUsed;

    public ServerAddHeaderAssertion(final AddHeaderAssertion assertion) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        final Map<String, ?> varMap = context.getVariableMap(variablesUsed, getAudit());
        final String name = ExpandVariables.process(assertion.getHeaderName(), varMap, getAudit());
        final String value = ExpandVariables.process(assertion.getHeaderValue(), varMap, getAudit());

        // TODO validate header name and value before setting

        final HeadersKnob headersKnob = message.getHeadersKnob();
        if (assertion.isRemoveExisting()) {
            if (headersKnob != null) {
                headersKnob.setHeader(name, value);
            } else {
                logger.log(Level.WARNING, "Cannot set header on message because headers knob is null.");
            }
        } else {
            if (headersKnob != null) {
                headersKnob.addHeader(name, value);
            } else {
                logger.log(Level.WARNING, "Cannot add header to message because headers knob is null.");
            }
        }

        return AssertionStatus.NONE;
    }
}
