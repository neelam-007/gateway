package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server side implementation of the PortalBootstrapAssertion.
 *
 * @see com.l7tech.external.assertions.portalbootstrap.PortalBootstrapAssertion
 */
public class ServerPortalBootstrapAssertion extends AbstractServerAssertion<PortalBootstrapAssertion> {

    public ServerPortalBootstrapAssertion( final PortalBootstrapAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }

    public static void onModuleUnloaded() {
    }
}
