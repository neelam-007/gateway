package com.l7tech.server.policy.assertion;

import com.l7tech.message.*;
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

        final HeadersKnob headersKnob = message.getHeadersKnob();
        if (assertion.isRemoveExisting()) {
            oh.setHeader(name, value);
            if (headersKnob != null) {
                headersKnob.setHeader(name, value);
            } else {
                logger.log(Level.WARNING, "Cannot set header on message because headers knob is null.");
            }
        } else {
            HttpRequestKnob hrk = message.getKnob(HttpRequestKnob.class);
            if (hrk != null && !oh.containsHeader(name)) {
                // If the message has an HttpRequestKnob, the presence of a header in the OutboundHeadersKnob
                // will block any existing values.  We'll need to copy them over to preserve them, the first time we shadow them,
                // if we are configured to not overwrite any existing values. (Bug #11365)
                String[] oldValues = hrk.getHeaderValues(name);
                for (String oldValue : oldValues) {
                    oh.addHeader(name, oldValue);
                }
            }

            oh.addHeader(name, value);
            if (headersKnob != null) {
                headersKnob.addHeader(name, value);
            } else {
                logger.log(Level.WARNING, "Cannot add header to message because headers knob is null.");
            }
        }

        return AssertionStatus.NONE;
    }
}
