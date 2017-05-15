package com.l7tech.external.assertions.jwt;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.DefaultAssertionAdvice;
import com.l7tech.policy.assertion.Assertion;

import java.util.Arrays;

/**
 * Policy change advice for DecodeJsonWebTokenAssertion.
 */
public class DecodeJsonWebTokenAssertionAdvice extends DefaultAssertionAdvice<DecodeJsonWebTokenAssertion> {
    @Override
    public void proceed(final PolicyChange pc) {
        if (pc != null) {
            final Assertion[] assertions = pc.getEvent().getChildren();
            if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof DecodeJsonWebTokenAssertion)) {
                throw new IllegalArgumentException("Expected one DecodeJsonWebTokenAssertion but received: " + Arrays.toString(assertions));
            } // TODO extract method argument validation to a parent class so logic can be reused
            final DecodeJsonWebTokenAssertion assertion = (DecodeJsonWebTokenAssertion) assertions[0];
            // US341928 change new instances of DecodeJsonWebTokenAssertion added into policy to fail if
            // signature is not verified while maintaining backwards compatibility
            assertion.setFailUnverifiedSignature(true);
            super.proceed(pc);
        }
    }
}
