package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.WsiBspAssertion;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class AddWsiBspAssertionAdvice extends AddContextSensitiveAssertionAdvice {

    //- PUBLIC

    public void proceed(PolicyChange pc) {
        super.proceed(pc);
        pc.proceed();
    }

    //- PROTECTED

    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof WsiBspAssertion)) throw new IllegalArgumentException("Expected WS-I BSP assertion.");
        WsiBspAssertion wsiBspAssertion = (WsiBspAssertion) assertion;
        wsiBspAssertion.setCheckRequestMessages(false);
        wsiBspAssertion.setCheckResponseMessages(true);
    }

    protected void notifyPreRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof WsiBspAssertion)) throw new IllegalArgumentException("Expected WS-I BSP assertion.");
        WsiBspAssertion wsiBspAssertion = (WsiBspAssertion) assertion;
        wsiBspAssertion.setCheckRequestMessages(true);
        wsiBspAssertion.setCheckResponseMessages(false);
    }
}
