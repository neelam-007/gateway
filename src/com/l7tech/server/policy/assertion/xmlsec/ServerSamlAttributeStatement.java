package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class <code>ServerSamlAttributeStatement</code> represents the server
 * side saml Authorization Statement policy assertion element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerSamlAttributeStatement implements ServerAssertion {
    private SamlAttributeStatement assertion;
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Create the server side saml security policy element
     *
     * @param sa the saml
     */
    public ServerSamlAttributeStatement(SamlAttributeStatement sa) {
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        assertion = sa;
    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {
        ProcessorResult wssResults;

        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

}
