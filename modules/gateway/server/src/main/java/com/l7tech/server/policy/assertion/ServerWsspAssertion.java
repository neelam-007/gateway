package com.l7tech.server.policy.assertion;

import java.io.IOException;

import org.springframework.context.ApplicationContext;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.WsspAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Server assertion for WSSP compliance.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class ServerWsspAssertion extends AbstractServerAssertion implements ServerAssertion {

    //- PUBLIC

    /**
     * Server assertion for WS-SecurityPolicy compliance.
     *
     * @param wsspAssertion assertion data object
     * @param springContext the application context to use
     */
    public ServerWsspAssertion(WsspAssertion wsspAssertion, ApplicationContext springContext) {
        super(wsspAssertion);
    }

    /**
     * Does nothing.
     *
     * @return AssertionStatus.NONE
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.NONE;
    }
}
