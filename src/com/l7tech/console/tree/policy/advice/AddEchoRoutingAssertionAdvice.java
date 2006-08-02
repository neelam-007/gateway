package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EchoRoutingAssertion;

/**
 * Advice to configure the initial values for the EchoRoutingAssertion.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class AddEchoRoutingAssertionAdvice implements Advice {

    //- PUBLIC

    public void proceed(PolicyChange pc) throws PolicyException {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof EchoRoutingAssertion)) {
            throw new IllegalArgumentException("Incorrect assertion type.");
        }

        EchoRoutingAssertion echoRoutingAssertion = (EchoRoutingAssertion) assertions[0];
        echoRoutingAssertion.setCurrentSecurityHeaderHandling(EchoRoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER);

        pc.proceed();
    }

}
