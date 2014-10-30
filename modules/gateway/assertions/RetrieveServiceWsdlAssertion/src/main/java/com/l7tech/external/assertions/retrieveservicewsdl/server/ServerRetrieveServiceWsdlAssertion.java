package com.l7tech.external.assertions.retrieveservicewsdl.server;

import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Server side implementation of the RetrieveServiceWsdlAssertion.
 *
 * @see com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class ServerRetrieveServiceWsdlAssertion extends AbstractServerAssertion<RetrieveServiceWsdlAssertion> {
    private final String[] variablesUsed;

    public ServerRetrieveServiceWsdlAssertion(final RetrieveServiceWsdlAssertion assertion) {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }
}
