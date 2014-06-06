package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HeadersKnobSupport;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Add/remove header(s) to/from a message.
 */
public class ServerAddHeaderAssertion extends AbstractMessageTargetableServerAssertion<AddHeaderAssertion> {
    private final String[] variablesUsed;

    // comma followed by an even number of double quotes (i.e. exclude quoted commas)
    private static final Pattern MULTIVALUED_HTTP_HEADER =
            Pattern.compile("(,\\s*)(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    public ServerAddHeaderAssertion(final AddHeaderAssertion assertion) {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message, final String messageDescription, final AuthenticationContext authContext)
            throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        final HeadersKnob headersKnob = message.getHeadersKnob();

        if (assertion.getHeaderName() == null) {
            throw new PolicyAssertionException(assertion, "Header name is null.");
        }

        final Map<String, ?> varMap = context.getVariableMap(variablesUsed, getAudit());
        final String name = ExpandVariables.process(assertion.getHeaderName(), varMap, getAudit());

        if (StringUtils.isBlank(name)) {
            status = AssertionStatus.FALSIFIED;
            logAndAudit(AssertionMessages.EMPTY_HEADER_NAME);
        } else {
            final String value = assertion.getHeaderValue() == null
                    ? null
                    : ExpandVariables.process(assertion.getHeaderValue(), varMap, getAudit());

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
                    throw new PolicyAssertionException(assertion, msg);
            }
        }

        return status;
    }

    private void doAdd(final HeadersKnob headersKnob,
                       final String assertionHeaderName, final String assertionHeaderValue) {
        if (assertion.isRemoveExisting()) {
            headersKnob.setHeader(assertionHeaderName, assertionHeaderValue, assertion.getMetadataType());
        } else {
            headersKnob.addHeader(assertionHeaderName, assertionHeaderValue, assertion.getMetadataType());
        }

        logAndAudit(AssertionMessages.HEADER_ADDED, assertionHeaderName, assertionHeaderValue);
    }

    private void doRemove(final HeadersKnob headersKnob,
                          final String assertionHeaderName, final String assertionHeaderValue) throws PolicyAssertionException {
        Pattern namePattern = null;

        try {
            if (assertion.isEvaluateNameAsExpression()) {
                namePattern = Pattern.compile(assertionHeaderName);
            }
        } catch (PatternSyntaxException e) {
            throw new PolicyAssertionException(assertion,
                    "Invalid regular expression: " + ExceptionUtils.getMessage(e));
        }

        if (StringUtils.isBlank(assertion.getHeaderValue())) {
            // don't care about header value
            if (assertion.isEvaluateNameAsExpression()) {
                for (final String headerName : headersKnob.getHeaderNames(assertion.getMetadataType())) {
                    if (namePattern.matcher(headerName).matches()) {
                        // when using an expression, we must match case
                        headersKnob.removeHeader(headerName, assertion.getMetadataType(), true);
                        logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME, headerName);
                    }
                }
            } else {
                if (headersKnob.containsHeader(assertionHeaderName, assertion.getMetadataType())) {
                    headersKnob.removeHeader(assertionHeaderName, assertion.getMetadataType());
                    logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME, assertionHeaderName);
                }
            }
        } else {
            // must match value in order to remove
            Pattern valuePattern = null;

            try {
                if (assertion.isEvaluateValueExpression()) {
                    valuePattern = Pattern.compile(assertionHeaderValue);
                }
            } catch (PatternSyntaxException e) {
                throw new PolicyAssertionException(assertion,
                        "Invalid regular expression: " + ExceptionUtils.getMessage(e));
            }

            final String[] headerNames;

            if (assertion.isEvaluateNameAsExpression()) {
                headerNames = headersKnob.getHeaderNames(assertion.getMetadataType(), true, false);
            } else {
                headerNames = headersKnob.getHeaderNames(assertion.getMetadataType(), true, true);
            }

            for (final String headerName : headerNames) {
                if ((!assertion.isEvaluateNameAsExpression() && assertionHeaderName.equalsIgnoreCase(headerName)) ||
                        (assertion.isEvaluateNameAsExpression() && namePattern.matcher(headerName).matches())) {
                    // name matches
                    for (final String headerValue : headersKnob.getHeaderValues(headerName, assertion.getMetadataType())) {
                        if (!headerValue.equalsIgnoreCase(assertionHeaderValue) && // if not an identical match ...
                                // (avoids false positives when a value separator is part of the actual value, but this
                                // is still susceptible for multivalued results where values which include separators)
                                headerValue.contains(HeadersKnobSupport.VALUE_SEPARATOR)) { // and contains a separator
                            // multivalued
                            final String[] subValues = MULTIVALUED_HTTP_HEADER.split(headerValue);

                            for (final String subValue : subValues) {
                                removeByValue(headersKnob, valuePattern, assertionHeaderValue, headerName, subValue.trim());
                            }
                        }

                        removeByValue(headersKnob, valuePattern, assertionHeaderValue, headerName, headerValue);
                    }
                }
            }
        }
    }

    private void removeByValue(final HeadersKnob headersKnob, final Pattern pattern,
                               final String valueToMatch, final String headerName, final String headerValue) {
        if ((!assertion.isEvaluateValueExpression() && valueToMatch.equalsIgnoreCase(headerValue)) ||
                (assertion.isEvaluateValueExpression() && pattern.matcher(headerValue).matches())) {
            // value matches
            // when using an expression, we must match case
            headersKnob.removeHeader(headerName, headerValue, assertion.getMetadataType(), assertion.isEvaluateNameAsExpression());
            logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE, headerName, headerValue);
        }
    }
}
