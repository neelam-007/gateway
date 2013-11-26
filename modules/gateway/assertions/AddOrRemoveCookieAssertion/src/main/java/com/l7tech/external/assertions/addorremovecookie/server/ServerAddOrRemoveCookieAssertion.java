package com.l7tech.external.assertions.addorremovecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.addorremovecookie.AddOrRemoveCookieAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server side implementation of the AddOrRemoveCookieAssertion.
 *
 * @see com.l7tech.external.assertions.addorremovecookie.AddOrRemoveCookieAssertion
 */
public class ServerAddOrRemoveCookieAssertion extends AbstractServerAssertion<AddOrRemoveCookieAssertion> {
    private final String[] variablesUsed;

    public ServerAddOrRemoveCookieAssertion(final AddOrRemoveCookieAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        validateRequiredFields(assertion.getOperation());
        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        switch (assertion.getOperation()) {
            case ADD:
                return doAdd(context, variableMap);
            case REMOVE:
                return doRemove(context, variableMap);
            default:
                throw new PolicyAssertionException(assertion, "Unsupported operation: " + assertion.getOperation());
        }
    }

    private AssertionStatus doRemove(final PolicyEnforcementContext context, final Map<String, Object> variableMap) {
        final String name = ExpandVariables.process(assertion.getName(), variableMap, getAudit());
        final List<HttpCookie> cookiesToRemove = new ArrayList<>();
        for (final HttpCookie cookie : context.getCookies()) {
            if (name.equals(cookie.getCookieName())) {
                cookiesToRemove.add(cookie);
            }
        }
        for (final HttpCookie cookie : cookiesToRemove) {
            context.deleteCookie(cookie);
            logAndAudit(AssertionMessages.COOKIE_REMOVED, cookie.getCookieName(), cookie.getCookieValue());
        }
        return AssertionStatus.NONE;
    }

    private AssertionStatus doAdd(final PolicyEnforcementContext context, final Map<String, Object> variableMap) throws PolicyAssertionException {
        final String name = ExpandVariables.process(assertion.getName(), variableMap, getAudit());
        final String value = ExpandVariables.process(assertion.getValue(), variableMap, getAudit());
        final String path = assertion.getCookiePath() == null ? null : ExpandVariables.process(assertion.getCookiePath(), variableMap, getAudit());
        final String domain = assertion.getDomain() == null ? null : ExpandVariables.process(assertion.getDomain(), variableMap, getAudit());
        final String maxAge = assertion.getMaxAge() == null ? null : ExpandVariables.process(assertion.getMaxAge(), variableMap, getAudit());
        final String comment = assertion.getComment() == null ? null : ExpandVariables.process(assertion.getComment(), variableMap, getAudit());
        Integer maxAgeInt;
        try {
            maxAgeInt = StringUtils.isBlank(maxAge) ? -1 : Integer.valueOf(maxAge);
        } catch (final NumberFormatException e) {
            logAndAudit(AssertionMessages.INVALID_MAX_AGE, maxAge);
            return AssertionStatus.FAILED;
        }
        context.addCookie(new HttpCookie(name, value, assertion.getVersion(), StringUtils.isBlank(path) ? null : path,
                StringUtils.isBlank(domain) ? null : domain, maxAgeInt, assertion.isSecure(),
                StringUtils.isBlank(comment) ? null : comment));
        logAndAudit(AssertionMessages.COOKIE_ADDED, name, value);
        return AssertionStatus.NONE;
    }

    private void validateRequiredFields(final AddOrRemoveCookieAssertion.Operation operation) throws PolicyAssertionException {
        if (assertion.getName() == null) {
            throw new PolicyAssertionException(assertion, "Cookie name is null");
        }
        if (operation == AddOrRemoveCookieAssertion.Operation.ADD) {
            if (assertion.getValue() == null) {
                throw new PolicyAssertionException(assertion, "Cookie value is null");
            }
        }
    }
}
