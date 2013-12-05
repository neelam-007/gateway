package com.l7tech.external.assertions.managecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server side implementation of the ManageCookieAssertion.
 *
 * @see com.l7tech.external.assertions.managecookie.ManageCookieAssertion
 */
public class ServerManageCookieAssertion extends AbstractServerAssertion<com.l7tech.external.assertions.managecookie.ManageCookieAssertion> {
    private final String[] variablesUsed;

    public ServerManageCookieAssertion(final com.l7tech.external.assertions.managecookie.ManageCookieAssertion assertion) throws PolicyAssertionException {
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
            case UPDATE:
                return doUpdate(context, variableMap);
            default:
                throw new PolicyAssertionException(assertion, "Unsupported operation: " + assertion.getOperation());
        }
    }

    private AssertionStatus doUpdate(final PolicyEnforcementContext context, final Map<String, Object> variableMap) {
        AssertionStatus status = AssertionStatus.NONE;
        final HttpCookie cookie = createCookie(variableMap);
        if (cookie != null) {
            if (removeCookiesByNameAndTarget(context, cookie.getCookieName())) {
                context.addCookie(cookie);
                logAndAudit(AssertionMessages.COOKIE_ADDED, cookie.getCookieName(), cookie.getCookieValue());
            } else {
                logAndAudit(AssertionMessages.COOKIE_NOT_FOUND, cookie.getCookieName(), assertion.getTarget().name());
                status = AssertionStatus.FALSIFIED;
            }
        } else {
            status = AssertionStatus.FAILED;
        }
        return status;
    }

    private AssertionStatus doRemove(final PolicyEnforcementContext context, final Map<String, Object> variableMap) {
        AssertionStatus status = AssertionStatus.NONE;
        final String name = ExpandVariables.process(assertion.getName(), variableMap, getAudit());
        if (StringUtils.isNotBlank(name)) {
            if (!removeCookiesByNameAndTarget(context, name)) {
                logAndAudit(AssertionMessages.COOKIE_NOT_FOUND, name, assertion.getTarget().name());
                status = AssertionStatus.FALSIFIED;
            }
        } else {
            logAndAudit(AssertionMessages.EMPTY_COOKIE_NAME);
            status = AssertionStatus.FAILED;
        }
        return status;
    }

    private AssertionStatus doAdd(final PolicyEnforcementContext context, final Map<String, Object> variableMap) throws PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        final HttpCookie cookie = createCookie(variableMap);
        if (cookie != null) {
            for (final HttpCookie existingCookie : context.getCookies()) {
                if (cookie.getCookieName().equals(existingCookie.getCookieName())) {
                    logAndAudit(AssertionMessages.COOKIE_ALREADY_EXISTS, cookie.getCookieName());
                    status = AssertionStatus.FALSIFIED;
                }
            }
            if (status == AssertionStatus.NONE) {
                context.addCookie(cookie);
                logAndAudit(AssertionMessages.COOKIE_ADDED, cookie.getCookieName(), cookie.getCookieValue());
            }
        } else {
            status = AssertionStatus.FAILED;
        }
        return status;
    }

    /**
     * Create a cookie from the assertion.
     *
     * @param variableMap the variable map to use for context variable resolution.
     * @return the created cookie or null if a cookie could not be created.
     */
    private HttpCookie createCookie(final Map<String, Object> variableMap) {
        HttpCookie cookie = null;
        final String name = ExpandVariables.process(assertion.getName(), variableMap, getAudit());
        if (StringUtils.isNotBlank(name)) {
            final String value = ExpandVariables.process(assertion.getValue(), variableMap, getAudit());
            final String path = assertion.getCookiePath() == null ? null : ExpandVariables.process(assertion.getCookiePath(), variableMap, getAudit());
            final String domain = assertion.getDomain() == null ? null : ExpandVariables.process(assertion.getDomain(), variableMap, getAudit());
            final String maxAge = assertion.getMaxAge() == null ? null : ExpandVariables.process(assertion.getMaxAge(), variableMap, getAudit());
            final String comment = assertion.getComment() == null ? null : ExpandVariables.process(assertion.getComment(), variableMap, getAudit());
            try {
                final Integer maxAgeInt = StringUtils.isBlank(maxAge) ? -1 : Integer.valueOf(maxAge);
                cookie = new HttpCookie(name, value, assertion.getVersion(), StringUtils.isBlank(path) ? null : path,
                        StringUtils.isBlank(domain) ? null : domain, maxAgeInt, assertion.isSecure(),
                        StringUtils.isBlank(comment) ? null : comment, assertion.getTarget() == TargetMessageType.RESPONSE);
            } catch (final NumberFormatException e) {
                logAndAudit(AssertionMessages.INVALID_MAX_AGE, maxAge);
            }
        } else {
            logAndAudit(AssertionMessages.EMPTY_COOKIE_NAME);
        }
        return cookie;
    }

    private void validateRequiredFields(final com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation operation) throws PolicyAssertionException {
        if (assertion.getTarget() == TargetMessageType.OTHER) {
            // for now we do not support TargetMessageType.OTHER until we move cookies to be stored on the message instead of the PEC.
            throw new PolicyAssertionException(assertion, "Unsupported target: " + assertion.getOtherTargetMessageVariable());
        }
        if (assertion.getName() == null) {
            throw new PolicyAssertionException(assertion, "Cookie name is null");
        }
        if (operation != com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.REMOVE) {
            if (assertion.getValue() == null) {
                throw new PolicyAssertionException(assertion, "Cookie value is null");
            }
        }
    }

    /**
     * @param context the PolicyEnforcementContext to remove cookies from.
     * @param name    the name of the cookie to remove.
     * @return true if at least one cookie was removed, false otherwise.
     */
    private boolean removeCookiesByNameAndTarget(final PolicyEnforcementContext context, final String name) {
        boolean removed = false;
        final List<HttpCookie> cookiesToRemove = new ArrayList<>();
        for (final HttpCookie cookie : context.getCookies()) {
            if (name.equals(cookie.getCookieName()) &&
                    (assertion.getTarget() == TargetMessageType.REQUEST && !cookie.isNew() ||
                            assertion.getTarget() == TargetMessageType.RESPONSE && cookie.isNew())) {
                cookiesToRemove.add(cookie);
            }
        }
        for (final HttpCookie cookie : cookiesToRemove) {
            context.deleteCookie(cookie);
            removed = true;
            logAndAudit(AssertionMessages.COOKIE_REMOVED, cookie.getCookieName(), cookie.getCookieValue());
        }
        return removed;
    }
}
