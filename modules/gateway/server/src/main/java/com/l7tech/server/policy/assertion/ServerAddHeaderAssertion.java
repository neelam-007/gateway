package com.l7tech.server.policy.assertion;

import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HeadersKnobSupport;
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
import java.util.regex.Pattern;

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
                    doAdd(headersKnob, name, value);
                    break;
                case REMOVE:
                    doRemove(headersKnob, name, value);
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

    private void doAdd(final HeadersKnob headersKnob, final String name, final String value) {
        if (assertion.isRemoveExisting()) {
            headersKnob.setHeader(name, value);
        } else {
            headersKnob.addHeader(name, value);
        }
        logger.log(Level.FINEST, "Added header with name=" + name + " and value=" + value);
    }

    private void doRemove(final HeadersKnob headersKnob, final String name, final String value) {
        if (!assertion.isMatchValueForRemoval()) {
            // don't care about header value
            if (assertion.isEvaluateNameAsExpression()) {
                final Pattern namePattern = Pattern.compile(name);
                for (final String headerName : headersKnob.getHeaderNames()) {
                    if (namePattern.matcher(headerName).matches()) {
                        // when using an expression, we must match case
                        headersKnob.removeHeader(headerName, true);
                        logger.log(Level.FINEST, "Removed header with name=" + headerName);
                    }
                }
            } else {
                headersKnob.removeHeader(name);
                logger.log(Level.FINEST, "Removed header with name=" + name);
            }
        } else {
            // must match value in order to remove
            for (final String headerName : headersKnob.getHeaderNames()) {
                if ((!assertion.isEvaluateNameAsExpression() && name.equalsIgnoreCase(headerName)) ||
                        Pattern.compile(name).matcher(headerName).matches()) {
                    // name matches
                    for (final String headerValue : headersKnob.getHeaderValues(headerName)) {
                        if (headerValue.contains(HeadersKnobSupport.VALUE_SEPARATOR)) {
                            // multivalued
                            final String[] subValues = StringUtils.split(headerValue, HeadersKnobSupport.VALUE_SEPARATOR);
                            for (final String subValue : subValues) {
                                removeByValue(headersKnob, value, headerName, subValue.trim());
                            }
                        }
                        removeByValue(headersKnob, value, headerName, headerValue);
                    }
                }
            }
        }
    }

    private void removeByValue(final HeadersKnob headersKnob, final String valueToMatch, final String headerName, final String headerValue) {
        if ((!assertion.isEvaluateValueExpression() && valueToMatch.equalsIgnoreCase(headerValue)) ||
                Pattern.compile(valueToMatch).matcher(headerValue).matches()) {
            // value matches
            // when using an expression, we must match case
            headersKnob.removeHeader(headerName, headerValue, true);
            logger.log(Level.FINEST, "Removed header with name=" + headerName + " and value=" + headerValue);
        }
    }
}
