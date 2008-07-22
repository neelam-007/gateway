/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.policy.assertion.credential.http;

import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class ClientCookieCredentialSourceAssertion extends ClientAssertion {
    private static final Logger logger = Logger.getLogger(ClientCookieCredentialSourceAssertion.class.getName());
    private final CookieCredentialSourceAssertion assertion;

    public ClientCookieCredentialSourceAssertion(CookieCredentialSourceAssertion assertion) {
        this.assertion = assertion;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
        String cookieName = assertion.getCookieName();
        if (cookieName == null || cookieName.length() < 1) {
            logger.warning("Unable to confirm client cookie -- policy has missing or empty cookie name");
            return AssertionStatus.SERVER_ERROR;
        }
        
        // If we are currently caching a cookie with the correct name, we will continue with this policy branch
        HttpCookie[] cookies = context.getSsg().getRuntime().getSessionCookies();
        for (int i = 0; i < cookies.length; i++) {
            HttpCookie cookie = cookies[i];
            if (cookieName.equals(cookie.getCookieName()))
                return AssertionStatus.NONE;
        }

        // No cookie by this name cached.  Fail this branch and try something else.
        return AssertionStatus.NOT_APPLICABLE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Require HTTP Cookie";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/authentication.gif";
    }
}
