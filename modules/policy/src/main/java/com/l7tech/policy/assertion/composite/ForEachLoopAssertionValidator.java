package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.AssertionValidatorSupport;
import com.l7tech.util.Functions;

/**
 * Policy validator for ConcurrentAllAssertion.
 */
public class ForEachLoopAssertionValidator extends AssertionValidatorSupport<ForEachLoopAssertion> {
    public ForEachLoopAssertionValidator(ForEachLoopAssertion assertion) {
        super(assertion);

        // Check for children that don't belong within a ConcurrentAllAssertion
        PolicyUtil.visitDescendantsAndSelf(assertion, new Functions.UnaryVoid<Assertion>() {
            @Override
            public void call(Assertion assertion) {
            }
        });
    }
}
