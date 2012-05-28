package com.l7tech.server.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerSetVariableAssertion;

import java.io.IOException;

public class ServerDelayAssertion extends ServerSetVariableAssertion {
    
    public ServerDelayAssertion(DelayAssertion assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        DelayAssertion delayAssertion = (DelayAssertion) assertion;
        try {
            delayAssertion.getTimeSource().sleep(delayAssertion.getDelay(), 0);
        } catch (InterruptedException e) {
        }

        return super.checkRequest(context);    //To change body of overridden methods use File | Settings | File Templates.
        
    }


}
