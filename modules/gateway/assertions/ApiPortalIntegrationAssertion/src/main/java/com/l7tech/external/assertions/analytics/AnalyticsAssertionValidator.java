package com.l7tech.external.assertions.analytics;

import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.AssertionValidatorSupport;
import com.l7tech.util.Functions;

/**
 * @author rraquepo, 7/8/14
 */
public class AnalyticsAssertionValidator extends AssertionValidatorSupport<AnalyticsAssertion> {
    public AnalyticsAssertionValidator(AnalyticsAssertion assertion) {
        super(assertion);

        // Check for children that don't belong within a ConcurrentAllAssertion
        PolicyUtil.visitDescendantsAndSelf(assertion, new Functions.UnaryVoid<Assertion>() {
            @Override
            public void call(Assertion assertion) {
            }
        });
    }
}
