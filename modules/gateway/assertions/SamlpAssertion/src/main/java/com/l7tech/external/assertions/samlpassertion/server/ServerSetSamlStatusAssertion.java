package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SetSamlStatusAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server assertion for Set SAML Status assertion
 */
public class ServerSetSamlStatusAssertion extends AbstractServerAssertion<SetSamlStatusAssertion> {

    //- PUBLIC

    public ServerSetSamlStatusAssertion( final SetSamlStatusAssertion assertion ) {
        super( assertion );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;

        if ( assertion.getVariableName()!=null && assertion.getSamlStatus() != null ) {
            context.setVariable( assertion.getVariableName(), assertion.getSamlStatus().getValue() );
        } else {
            logger.warning( "Invalid assertion configuration, missing variable name or status." );
            status = AssertionStatus.FAILED;
        }
        return status;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerSetSamlStatusAssertion.class.getName());
}
