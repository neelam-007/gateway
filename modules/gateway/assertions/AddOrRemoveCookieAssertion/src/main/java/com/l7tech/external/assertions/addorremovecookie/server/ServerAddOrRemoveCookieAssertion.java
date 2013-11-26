package com.l7tech.external.assertions.addorremovecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.addorremovecookie.AddOrRemoveCookieAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;
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
        validateRequiredFields();
        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        final String name = ExpandVariables.process(assertion.getName(), variableMap, getAudit());
        final String value = ExpandVariables.process(assertion.getValue(), variableMap, getAudit());
        final String version = ExpandVariables.process(assertion.getVersion(), variableMap, getAudit());
        final String path = assertion.getCookiePath() == null ? null : ExpandVariables.process(assertion.getCookiePath(), variableMap, getAudit());
        final String domain = assertion.getDomain() == null ? null : ExpandVariables.process(assertion.getDomain(), variableMap, getAudit());
        final String maxAge = assertion.getMaxAge() == null ? null : ExpandVariables.process(assertion.getMaxAge(), variableMap, getAudit());
        final String comment = assertion.getComment() == null ? null : ExpandVariables.process(assertion.getComment(), variableMap, getAudit());
        Integer versionInt;
        try {
            versionInt = Integer.valueOf(version);
        } catch (final NumberFormatException e) {
            logAndAudit(AssertionMessages.INVALID_COOKIE_VERSION, version);
            return AssertionStatus.FAILED;
        }
        Integer maxAgeInt;
        try {
            maxAgeInt = maxAge == null ? -1 : Integer.valueOf(maxAge);
        } catch (final NumberFormatException e) {
            logAndAudit(AssertionMessages.INVALID_MAX_AGE, maxAge);
            return AssertionStatus.FAILED;
        }
        context.addCookie(new HttpCookie(name, value, versionInt, path, domain, maxAgeInt, assertion.isSecure(), comment));
        return AssertionStatus.NONE;
    }

    private void validateRequiredFields() throws PolicyAssertionException {
        if (assertion.getName() == null) {
            throw new PolicyAssertionException(assertion, "Cookie name is null");
        }
        if (assertion.getValue() == null) {
            throw new PolicyAssertionException(assertion, "Cookie value is null");
        }
        if (assertion.getVersion() == null) {
            throw new PolicyAssertionException(assertion, "Cookie version is null");
        }
    }
}
