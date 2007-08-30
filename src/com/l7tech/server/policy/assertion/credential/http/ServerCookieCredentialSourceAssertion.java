/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.server.audit.Auditor;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class ServerCookieCredentialSourceAssertion extends ServerCredentialSourceAssertion {
    private static final Logger logger = Logger.getLogger(ServerCookieCredentialSourceAssertion.class.getName());
    private final String cookieName;
    private final Auditor auditor;

    public ServerCookieCredentialSourceAssertion(CookieCredentialSourceAssertion data, ApplicationContext springContext) {
        super(data, springContext);
        this.cookieName = data.getCookieName();
        auditor = new Auditor(this, springContext, logger);
    }

    protected LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
        HttpRequestKnob hrk = request.getHttpRequestKnob();
        HttpCookie[] cookies = hrk.getCookies();
        for (int i = 0; i < cookies.length; i++) {
            HttpCookie cookie = cookies[i];
            if (cookieName.equalsIgnoreCase(cookie.getCookieName())) {
                String cookieValue = cookie.getCookieValue();
                //String login = "cookie-" + cookieName + "-" + HexUtils.encodeBase64(cookieValue.getBytes("UTF-8"), true);
                logger.log(Level.FINE, "Found cookie with the name: {0}", cookieName);
                return new LoginCredentials(null, cookieValue.toCharArray(), CredentialFormat.OPAQUETOKEN, getClass());
            }
        }

        logger.log(Level.FINE, "No cookie found with the name: {0}", cookieName);
        return null;
    }

    protected AssertionStatus checkCredentials(LoginCredentials pc, Map authParams) throws CredentialFinderException {
        if ( pc == null ) return AssertionStatus.AUTH_REQUIRED;

        char[] cookie = pc.getCredentials();
        if (cookie == null || cookie.length < 1)
            throw new CredentialFinderException("Session cookie is missing or empty");

        // Can't check anything else -- the content of the session cookie are opaque to us
        return AssertionStatus.NONE;
    }

    protected void challenge(PolicyEnforcementContext context, Map authParams) {
        // No challenge required -- request either included the cookie or it didn't; either way,
        // nothing this assertion can do about it.  It's the custom assertion's job to set any required
        // session cookies on the response.
    }
}
