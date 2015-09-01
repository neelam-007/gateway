package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalEncassIntegrationAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * Server side implementation of the ApiPortalIntegrationAssertion.
 *
 * @author Victor Kazakov
 * @see com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion
 */
public class ServerApiPortalEncassIntegrationAssertion extends AbstractServerAssertion<ApiPortalEncassIntegrationAssertion> {

    private final ApiPortalEncassIntegrationAssertion assertion;

    public ServerApiPortalEncassIntegrationAssertion(final ApiPortalEncassIntegrationAssertion assertion, final ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.assertion = assertion;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.NONE;
    }
}
