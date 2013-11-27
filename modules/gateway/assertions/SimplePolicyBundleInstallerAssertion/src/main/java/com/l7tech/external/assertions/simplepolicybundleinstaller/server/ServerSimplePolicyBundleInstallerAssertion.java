package com.l7tech.external.assertions.simplepolicybundleinstaller.server;

import com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Server side implementation of the SimplePolicyBundleInstallerAssertion.
 * Used to load the Tasks > Additional Task menu to install the SimplePolicyBundleInstaller sample.
 *
 * @see com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAssertion
 */
public class ServerSimplePolicyBundleInstallerAssertion extends AbstractServerAssertion<SimplePolicyBundleInstallerAssertion> {

    public ServerSimplePolicyBundleInstallerAssertion( final SimplePolicyBundleInstallerAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }
}
