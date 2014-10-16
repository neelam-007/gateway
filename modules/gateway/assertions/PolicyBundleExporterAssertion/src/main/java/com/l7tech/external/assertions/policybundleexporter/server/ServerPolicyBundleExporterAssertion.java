package com.l7tech.external.assertions.policybundleexporter.server;

import com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Do nothing server side implementation.
 *
 * @see com.l7tech.external.assertions.policybundleexporter.PolicyBundleExporterAssertion
 */
public class ServerPolicyBundleExporterAssertion extends AbstractServerAssertion<PolicyBundleExporterAssertion> {

    public ServerPolicyBundleExporterAssertion( final PolicyBundleExporterAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }
}
