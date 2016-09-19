package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpCookiesKnob;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author mike
 */
public class ServerCookieCredentialSourceAssertion extends ServerCredentialSourceAssertion<CookieCredentialSourceAssertion> {
    private final String cookieName;

    public ServerCookieCredentialSourceAssertion(CookieCredentialSourceAssertion data) {
        super(data);
        this.cookieName = data.getCookieName();
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final AssertionStatus assertionStatus = super.checkRequest(context);
        if (AssertionStatus.NONE.equals(assertionStatus)) {
            if (StringUtils.isNotBlank(assertion.getVariablePrefix())) {
                // consider more specific knob for cookies
                // set cookie context variable
                for (final HttpCookie cookie : context.getRequest().getHttpCookiesKnob().getCookies()) {
                    if (cookie.getCookieName().equals(cookieName)) {
                        context.setVariable(assertion.getVariablePrefix() + "." + cookie.getCookieName(), cookie.getCookieValue());
                        break;
                    }
                }
            }else{
                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, "No variable prefix specified. Cannot set cookie context variable.");
            }
        }
        return assertionStatus;
    }

    @Override
    protected LoginCredentials findCredentials(Message request, Map<String, String> authParams) throws IOException, CredentialFinderException {
        // consider more specific knob for cookies
        HttpCookiesKnob hck = request.getHttpCookiesKnob();
        for (final HttpCookie cookie : hck.getCookies()) {
            if (cookieName.equalsIgnoreCase(cookie.getCookieName())) {
                final String cookieValue = cookie.getCookieValue();
                if (cookieValue != null && cookieValue.length() > 0) {
                    logAndAudit(AssertionMessages.HTTPCOOKIE_FOUND, cookieName);
                    return LoginCredentials.makeLoginCredentials(new OpaqueSecurityToken(null, cookieValue.toCharArray()), CookieCredentialSourceAssertion.class);
                } else {
                    logAndAudit(AssertionMessages.HTTPCOOKIE_FOUND_EMPTY, cookieName);
                    return null;
                }
            }
        }

        logAndAudit(AssertionMessages.HTTPCOOKIE_NOT_FOUND, cookieName);
        return null;
    }

    @Override
    protected AssertionStatus checkCredentials(LoginCredentials pc, Map<String, String> authParams) throws CredentialFinderException {
        if (pc == null) return AssertionStatus.AUTH_REQUIRED;

        char[] cookie = pc.getCredentials();
        if (cookie == null || cookie.length < 1)
            throw new CredentialFinderException("Session cookie is missing or empty");

        // Can't check anything else -- the content of the session cookie are opaque to us
        return AssertionStatus.NONE;
    }

    @Override
    protected void challenge(PolicyEnforcementContext context, Map<String, String> authParams) {
        // No challenge required -- request either included the cookie or it didn't; either way,
        // nothing this assertion can do about it.  It's the custom assertion's job to set any required
        // session cookies on the response.
    }
}
