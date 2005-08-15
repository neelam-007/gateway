/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.DropConnection;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyInterruptedException;

import java.io.IOException;

import org.springframework.context.ApplicationContext;

/**
 * This assertion interrupts the policy execution and instruct the http transport
 * sub-system to drop the connection with the requestor.
 *
 * @author flascelles@layer7-tech.com
 */
public class ServerDropConnection implements ServerAssertion {

    public ServerDropConnection(DropConnection assertion, ApplicationContext context) {}

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setStealthResponseMode(true);
        throw new PolicyInterruptedException("Interrupting policy execution.");
    }
}
