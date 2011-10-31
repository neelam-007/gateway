/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.Advice;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion;
import com.l7tech.policy.assertion.Assertion;

/**
 * Advice to enable a default value of false for 'validateWebSsoRules' property for for new instances of the
 * SamlpResponseBuilderAssertion assertion.
 */
public class SamlpResponseBuilderAssertionAdvice implements Advice {

    @Override
    public void proceed(PolicyChange pc) {

        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SamlpResponseBuilderAssertion)) {
            throw new IllegalArgumentException();
        }

        SamlpResponseBuilderAssertion assertion = (SamlpResponseBuilderAssertion) assertions[0];
        // set to false for new assertions - override default property value of true.
        assertion.setValidateWebSsoRules(false);
        pc.proceed();
    }
}
