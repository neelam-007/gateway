package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server side implementation of the OdataValidationAssertion.
 *
 * @see com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion
 */
public class ServerOdataValidationAssertion extends AbstractServerAssertion<OdataValidationAssertion> {
    private final String[] variablesUsed;

// DELETEME example for dependency injection
//    @Inject
//    @Named("foo") -- The name is not usually required and should be left out if possible
//    private Foo foo;

    public ServerOdataValidationAssertion( final OdataValidationAssertion assertion ) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     *
     * DELETEME if not required.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }
}
