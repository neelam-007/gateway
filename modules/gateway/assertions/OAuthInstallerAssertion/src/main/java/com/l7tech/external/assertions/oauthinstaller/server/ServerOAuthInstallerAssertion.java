package com.l7tech.external.assertions.oauthinstaller.server;

import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Server side implementation of the OAuthInstallerAssertion.
 *
 * This assertion is used to load the new Tools task to install the OTK.
 *
 * @see com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion
 */
public class ServerOAuthInstallerAssertion extends AbstractServerAssertion<OAuthInstallerAssertion> {

    public ServerOAuthInstallerAssertion( final OAuthInstallerAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }
}
