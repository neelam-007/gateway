package com.l7tech.server.policy.assertion;

import com.l7tech.message.HasOutboundHeaders;
import com.l7tech.message.HttpOutboundRequestFacet;
import com.l7tech.message.Message;
import com.l7tech.message.OutboundHeadersKnob;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;
import java.util.Map;

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
            throws IOException, PolicyAssertionException
    {
        HasOutboundHeaders oh = message.getKnob(OutboundHeadersKnob.class);
        if (oh == null) {
            // Message is not already configured to store outbound headers.
            // If this is the default Response, the request presumably arrived over a one-way transport.
            // In any case, we will assume the message will eventually be sent somewhere else as a request,
            // and attach a new OutboundRequestKnob.
            oh = HttpOutboundRequestFacet.getOrCreateHttpOutboundRequestKnob(message);
        }

        Map<String, ?> varMap = context.getVariableMap(variablesUsed, getAudit());
        final String name = ExpandVariables.process(assertion.getHeaderName(), varMap, getAudit());
        final String value = ExpandVariables.process(assertion.getHeaderValue(), varMap, getAudit());

        // TODO validate header name and value before setting

        if (assertion.isRemoveExisting()) {
            oh.setHeader(name, value);
        } else {
            oh.addHeader(name, value);
        }

        return AssertionStatus.NONE;
    }
}
