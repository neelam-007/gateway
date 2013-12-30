package com.l7tech.external.assertions.simplepolicybundleinstaller.server;

import com.l7tech.external.assertions.simplepolicybundleinstaller.SimpleAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Rename SimpleAssertion and ServerSimpleAssertion to test the logic that checks if an Assertion (specified in Assertion.xml) exists on Gateway.
 * For example rename to SimpleAssertionBackup and ServerSimpleAssertionBackup.
 *
 * @see com.l7tech.external.assertions.simplepolicybundleinstaller.SimpleAssertion
 */
public class ServerSimpleAssertion extends AbstractServerAssertion<SimpleAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSimpleAssertion.class.getName());

    public ServerSimpleAssertion(final SimpleAssertion assertion) throws PolicyAssertionException {
        super(assertion);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        logger.info("ServerSimpleAssertion called");
        return AssertionStatus.NONE;
    }
}
