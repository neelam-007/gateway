package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;

/**
 * Class <code>ServerSamlSecurity</code> represents the server side saml
 * security policy assertion element.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerSamlSecurity implements ServerAssertion {
    private SamlSecurity assertion;

    /**
     * Create the server side saml security policy element
     * @param sa the saml
     */
    public ServerSamlSecurity(SamlSecurity sa) {
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        assertion = sa;
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param request  (In/Out) The request to check.  May be modified by processing.
     * @param response (Out) The response to send back.  May be replaced during processing.
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        return AssertionStatus.FALSIFIED;
    }
}
