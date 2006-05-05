package com.l7tech.server.policy.assertion;

import org.springframework.context.ApplicationContext;
import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * Server side implementation of the FaultLevel assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 5, 2006<br/>
 *
 * @see com.l7tech.policy.assertion.FaultLevel
 */
public class ServerFaultLevel {
    public ServerFaultLevel(FaultLevel assertion, ApplicationContext context) {}

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        return AssertionStatus.NONE;
    }
}
