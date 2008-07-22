package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.WsiSamlAssertion;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class AddWsiSamlAssertionAdvice extends AddContextSensitiveAssertionAdvice {

    //- PUBLIC

    public void proceed(PolicyChange pc) {
        super.proceed(pc);
        pc.proceed();
    }

    //- PROTECTED

    protected void notifyPostRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof WsiSamlAssertion)) throw new IllegalArgumentException("Expected WS-I SAML assertion.");
        WsiSamlAssertion wsiSamlAssertion = (WsiSamlAssertion) assertion;
        wsiSamlAssertion.setCheckRequestMessages(false);
        wsiSamlAssertion.setCheckResponseMessages(true);
    }

    protected void notifyPreRouting(PolicyChange pc, Assertion assertion) {
        if (!(assertion instanceof WsiSamlAssertion)) throw new IllegalArgumentException("Expected WS-I SAML assertion.");
        WsiSamlAssertion wsiSamlAssertion = (WsiSamlAssertion) assertion;
        wsiSamlAssertion.setCheckRequestMessages(true);
        wsiSamlAssertion.setCheckResponseMessages(false);
    }
}
