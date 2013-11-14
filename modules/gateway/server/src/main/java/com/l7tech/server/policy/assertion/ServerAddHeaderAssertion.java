package com.l7tech.server.policy.assertion;

import com.l7tech.message.HeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.apache.commons.lang.StringUtils;

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
        final HeadersKnob headersKnob = message.getHeadersKnob();
        if (headersKnob != null) {
            if (assertion.getHeaderName() == null) {
                throw new PolicyAssertionException(assertion, "Header name is null.");
            }
            final Map<String, ?> varMap = context.getVariableMap(variablesUsed, getAudit());
            final String name = ExpandVariables.process(assertion.getHeaderName(), varMap, getAudit());
            final String value = assertion.getHeaderValue() == null ? null : ExpandVariables.process(assertion.getHeaderValue(), varMap, getAudit());

            // TODO validate header name and value before setting

            switch (assertion.getOperation()) {
                case ADD:
                    if (assertion.isRemoveExisting()) {
                        headersKnob.setHeader(name, value);
                    } else {
                        headersKnob.addHeader(name, value);
                    }
                    logger.log(Level.FINEST, "Added header with name=" + name + " and value=" + value);
                    break;
                case REMOVE:
                    if (assertion.isMatchValueForRemoval()) {
                        headersKnob.removeHeader(name, value);
                        logger.log(Level.FINEST, "Removed header with name=" + name + " and value=" + value);
                    } else {
                        headersKnob.removeHeader(name);
                        logger.log(Level.FINEST, "Removed header with name=" + name);
                    }
                    break;
                default:
                    final String msg = "Unsupported operation: " + assertion.getOperation();
                    logger.log(Level.WARNING, msg);
                    throw new IllegalArgumentException(msg);
            }
        } else {
            throw new IllegalStateException("HeadersKnob on message is null. Message may not have been initialized.");
        }

        return AssertionStatus.NONE;
    }
}
