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
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Add/remove header(s) to/from a message or cookie(s) to/from context via Cookie/Set-Cookie header.
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
                    doAdd(headersKnob, name, value, context);
                    break;
                case REMOVE:
                    doRemove(headersKnob, name, value, context);
                    break;
                default:
                    final String msg = "Unsupported operation: " + assertion.getOperation();
                    logger.log(Level.WARNING, msg);
                    throw new PolicyAssertionException(assertion, msg);
            }
        } else {
            throw new IllegalStateException("HeadersKnob on message is null. Message may not have been initialized.");
        }

        return AssertionStatus.NONE;
    }

    private void doAdd(final HeadersKnob headersKnob, final String assertionHeaderName, final String assertionHeaderValue, final PolicyEnforcementContext context) {
        final boolean isCookieHeader = assertionHeaderName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE);
        final boolean isSetCookieHeader = assertionHeaderName.equalsIgnoreCase(HttpConstants.HEADER_SET_COOKIE);
        if ((isCookieHeader && assertion.getTarget() == TargetMessageType.REQUEST) ||
                (isSetCookieHeader && assertion.getTarget() == TargetMessageType.RESPONSE)) {
            if (assertion.isRemoveExisting()) {
                // delete all existing cookies from context
                removeCookiesFromContext(context.getCookies(), context);
                // remove cookie/set-cookie header if there happens to be any
                if (headersKnob.containsHeader(assertionHeaderName)) {
                    headersKnob.removeHeader(assertionHeaderName);
                    logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME, assertionHeaderName);
                }
            }
            // add cookie to context
            // all cookie attributes are parsed from the header value
            try {
                final HttpCookie cookie = new HttpCookie(null, null, assertionHeaderValue, assertion.getTarget() == TargetMessageType.RESPONSE);
                context.addCookie(cookie);
                logAndAudit(AssertionMessages.COOKIE_ADDED, cookie.getCookieName(), cookie.getCookieValue());
            } catch (final HttpCookie.IllegalFormatException e) {
                logAndAudit(AssertionMessages.HTTPROUTE_INVALIDCOOKIE, assertionHeaderValue);
            }
        } else {
            if (isCookieHeader || isSetCookieHeader) {
                logAndAudit(AssertionMessages.INVALID_HEADER_FOR_TARGET, assertionHeaderName, assertion.getTarget().toString());
            }

            if (assertion.isRemoveExisting()) {
                headersKnob.setHeader(assertionHeaderName, assertionHeaderValue);
            } else {
                headersKnob.addHeader(assertionHeaderName, assertionHeaderValue);
            }
            logAndAudit(AssertionMessages.HEADER_ADDED, assertionHeaderName, assertionHeaderValue);
        }
    }

    private void doRemove(final HeadersKnob headersKnob, final String assertionHeaderName, final String assertionHeaderValue, final PolicyEnforcementContext context) {
        if (!assertion.isMatchValueForRemoval()) {
            // don't care about header value
            if (assertion.isEvaluateNameAsExpression()) {
                final Pattern namePattern = Pattern.compile(assertionHeaderName);
                for (final String headerName : headersKnob.getHeaderNames()) {
                    if (namePattern.matcher(headerName).matches()) {
                        // when using an expression, we must match case
                        headersKnob.removeHeader(headerName, true);
                        logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME, headerName);
                    }
                }
            } else {
                if (headersKnob.containsHeader(assertionHeaderName)) {
                    headersKnob.removeHeader(assertionHeaderName);
                    logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME, assertionHeaderName);
                }
            }
        } else {
            // must match value in order to remove
            for (final String headerName : headersKnob.getHeaderNames()) {
                if ((!assertion.isEvaluateNameAsExpression() && assertionHeaderName.equalsIgnoreCase(headerName)) ||
                        Pattern.compile(assertionHeaderName).matcher(headerName).matches()) {
                    // name matches
                    for (final String headerValue : headersKnob.getHeaderValues(headerName)) {
                        if (headerValue.contains(HeadersKnobSupport.VALUE_SEPARATOR)) {
                            // multivalued
                            final String[] subValues = StringUtils.split(headerValue, HeadersKnobSupport.VALUE_SEPARATOR);
                            for (final String subValue : subValues) {
                                removeByValue(headersKnob, assertionHeaderValue, headerName, subValue.trim(), context);
                            }
                        }
                        removeByValue(headersKnob, assertionHeaderValue, headerName, headerValue, context);
                    }
                }
            }
        }
        doRemoveCookies(assertionHeaderName, assertionHeaderValue, context);
    }

    /**
     * Cookies will only be removed from the context if header name + target pair is Cookie + request OR Set-Cookie + response.
     */
    private void doRemoveCookies(final String assertionHeaderName, final String assertionHeaderValue, final PolicyEnforcementContext context) {
        boolean cookieHeaderNameMatch = false;
        if (assertion.isEvaluateNameAsExpression()) {
            final Pattern namePattern = Pattern.compile(assertionHeaderName);
            if ((namePattern.matcher(HttpConstants.HEADER_COOKIE).matches() && assertion.getTarget() == TargetMessageType.REQUEST) ||
                    (namePattern.matcher(HttpConstants.HEADER_SET_COOKIE).matches() && assertion.getTarget() == TargetMessageType.RESPONSE)) {
                cookieHeaderNameMatch = true;
            }
        } else {
            final boolean isCookieHeader = assertionHeaderName.equalsIgnoreCase(HttpConstants.HEADER_COOKIE);
            final boolean isSetCookieHeader = assertionHeaderName.equalsIgnoreCase(HttpConstants.HEADER_SET_COOKIE);
            if ((isCookieHeader && assertion.getTarget() == TargetMessageType.REQUEST) ||
                    (isSetCookieHeader && assertion.getTarget() == TargetMessageType.RESPONSE)) {
                cookieHeaderNameMatch = true;
            } else if (isCookieHeader || isSetCookieHeader) {
                logAndAudit(AssertionMessages.INVALID_HEADER_FOR_TARGET, assertionHeaderName, assertion.getTarget().toString());
            }
        }

        if (cookieHeaderNameMatch) {
            if (assertion.isMatchValueForRemoval()) {
                // only remove cookies with values that match
                final List<HttpCookie> cookiesToRemove = new ArrayList<>();
                for (final HttpCookie cookie : context.getCookies()) {
                    final String cookieValue = HttpCookie.getCookieHeader(Collections.singleton(cookie));
                    if ((!assertion.isEvaluateValueExpression() && assertionHeaderValue.equals(cookieValue) || // equality check
                            assertion.isEvaluateValueExpression() && Pattern.compile(assertionHeaderValue).matcher(cookieValue).matches())) { // expression check
                        cookiesToRemove.add(cookie);
                    }
                }
                removeCookiesFromContext(cookiesToRemove, context);
            } else {
                // value doesn't matter - remove all cookies
                removeCookiesFromContext(context.getCookies(), context);
            }
        }
    }

    private void removeByValue(final HeadersKnob headersKnob, final String valueToMatch, final String headerName, final String headerValue, final PolicyEnforcementContext context) {
        if ((!assertion.isEvaluateValueExpression() && valueToMatch.equalsIgnoreCase(headerValue)) ||
                Pattern.compile(valueToMatch).matcher(headerValue).matches()) {
            // value matches
            // when using an expression, we must match case
            headersKnob.removeHeader(headerName, headerValue, true);
            logAndAudit(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE, headerName, headerValue);
        }
    }

    private void removeCookiesFromContext(final Collection<HttpCookie> cookiesToRemove, final PolicyEnforcementContext context) {
        for (final HttpCookie toRemove : cookiesToRemove) {
            context.deleteCookie(toRemove);
            logAndAudit(AssertionMessages.COOKIE_REMOVED, toRemove.getCookieName(), toRemove.getCookieValue());
        }
    }
}
