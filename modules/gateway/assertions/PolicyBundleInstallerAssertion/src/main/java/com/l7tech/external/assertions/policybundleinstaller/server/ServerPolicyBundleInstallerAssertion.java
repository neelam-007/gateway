package com.l7tech.external.assertions.policybundleinstaller.server;

import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Server side implementation of the PolicyBundleInstallerAssertion.
 *
 * @see com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerAssertion
 */
public class ServerPolicyBundleInstallerAssertion extends AbstractServerAssertion<PolicyBundleInstallerAssertion> {
    private final String[] variablesUsed;

    public ServerPolicyBundleInstallerAssertion( final PolicyBundleInstallerAssertion assertion ) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }

}
