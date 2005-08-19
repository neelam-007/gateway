/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.StealthFault;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

import org.springframework.context.ApplicationContext;

/**
 * This assertion flags the policy context so that if the policy results in an error (policy is not succesfull) then
 * the transport does not return an error but drops the client connection instead.
 *
 * @author flascelles@layer7-tech.com
 */
public class ServerStealthFault implements ServerAssertion {

    public ServerStealthFault(StealthFault assertion, ApplicationContext context) {}

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // todo, log this
        context.setStealthResponseMode(true);
        //throw new PolicyInterruptedException("Interrupting policy execution.");
        return AssertionStatus.NONE;
    }
}
