package com.l7tech.external.assertions.simplepolicybundleinstaller.server;

import com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerAbstractServerAssertion;
import org.springframework.context.ApplicationContext;

/**
 * Server side implementation of the SimplePolicyBundleInstallerAssertion.
 *
 * @see com.l7tech.external.assertions.simplepolicybundleinstaller.SimplePolicyBundleInstallerAssertion
 */
public class ServerSimplePolicyBundleInstallerAssertion extends PolicyBundleInstallerAbstractServerAssertion<SimplePolicyBundleInstallerAssertion> {

    public ServerSimplePolicyBundleInstallerAssertion( final SimplePolicyBundleInstallerAssertion assertion,
                                                       final ApplicationContext applicationContext ) throws PolicyAssertionException {
        super(assertion, applicationContext);
    }
}
