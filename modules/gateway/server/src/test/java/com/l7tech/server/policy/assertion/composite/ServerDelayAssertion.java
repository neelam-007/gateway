package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerSetVariableAssertion;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: awitrisna
 * Date: 23/04/12
 * Time: 2:51 PM
 */
public class ServerDelayAssertion extends ServerSetVariableAssertion {
    
    public ServerDelayAssertion(SetVariableAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            Thread.sleep(((DelayAssertion) assertion).getDelay());
        } catch (InterruptedException e) {
        }

        return super.checkRequest(context);    //To change body of overridden methods use File | Settings | File Templates.
        
    }


}
