package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
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
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message, final String messageDescription, final AuthenticationContext authContext)
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
                    try {
                        doAdd(headersKnob, name, value, context);
                    } catch (final HttpCookie.IllegalFormatException e) {
                        logAndAudit(AssertionMessages.HTTPROUTE_INVALIDCOOKIE, value);
                        return AssertionStatus.FALSIFIED;
                    }
                    break;
                case REMOVE:
                    doRemove(headersKnob, name, value, context);
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

    private void doAdd(final HeadersKnob headersKnob, final String name, final String value, final PolicyEnforcementContext context) throws HttpCookie.IllegalFormatException {
        if (assertion.isRemoveExisting()) {
            headersKnob.setHeader(name, value);
        } else {
            headersKnob.addHeader(name, value);
        }
        if (name.equalsIgnoreCase(HttpConstants.HEADER_COOKIE)) {
            // all cookie attributes are parsed from the header value
            context.addCookie(new HttpCookie((String) null, null, value));
        }
        logAndAudit(AssertionMessages.HEADER_ADDED, name, value);

    }

    private void doRemove(final HeadersKnob headersKnob, final String name, final String value, final PolicyEnforcementContext context) {
        if (!assertion.isMatchValueForRemoval()) {
            // don't care about header value
            if (assertion.isEvaluateNameAsExpression()) {
                final Pattern namePattern = Pattern.compile(name);
                for (final String headerName : headersKnob.getHeaderNames()) {
                    if (namePattern.matcher(headerName).matches()) {
                        // when using an expression, we must match case
                        headersKnob.removeHeader(headerName, true);
                        if (HttpConstants.HEADER_COOKIE.equalsIgnoreCase(headerName)) {
                            removeAllCookiesFromContext(context);
                        }
                        logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME, headerName);
                    }
                }
            } else if (headersKnob.containsHeader(name)) {
                headersKnob.removeHeader(name);
                if (HttpConstants.HEADER_COOKIE.equalsIgnoreCase(name)) {
                    removeAllCookiesFromContext(context);
                }
                logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME, name);
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
                                removeByValue(headersKnob, value, headerName, subValue.trim(), context);
                            }
                        }
                        removeByValue(headersKnob, value, headerName, headerValue, context);
                    }
                }
            }
        }
    }

    private void removeByValue(final HeadersKnob headersKnob, final String valueToMatch, final String headerName, final String headerValue, final PolicyEnforcementContext context) {
        if ((!assertion.isEvaluateValueExpression() && valueToMatch.equalsIgnoreCase(headerValue)) ||
                Pattern.compile(valueToMatch).matcher(headerValue).matches()) {
            // value matches
            // when using an expression, we must match case
            headersKnob.removeHeader(headerName, headerValue, true);
            if (HttpConstants.HEADER_COOKIE.equalsIgnoreCase(headerName)) {
                removeCookiesFromContext(headerValue, context);
            }
            logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE, headerName, headerValue);
        }
    }

    private void removeAllCookiesFromContext(final PolicyEnforcementContext context) {
        for (final HttpCookie cookie : context.getCookies()) {
            context.deleteCookie(cookie);
        }
    }

    private void removeCookiesFromContext(final String cookieHeaderValue, final PolicyEnforcementContext context) {
        try {
            final HttpCookie parsed = new HttpCookie((String) null, null, cookieHeaderValue);
            final List<HttpCookie> toDelete = new ArrayList<>();
            for (final HttpCookie inContext : context.getCookies()) {
                if (inContext.getCookieName().equals(parsed.getCookieName()) && inContext.getCookieValue().equals(parsed.getCookieValue())) {
                    toDelete.add(inContext);
                }
            }
            for (final HttpCookie delete : toDelete) {
                context.deleteCookie(delete);
            }
        } catch (final HttpCookie.IllegalFormatException e) {
            logger.log(Level.WARNING, "Unable to parse cookie from header value: " + cookieHeaderValue);
        }
    }
}
