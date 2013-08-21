package com.l7tech.external.assertions.xmlsec;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.DefaultAssertionAdvice;
import com.l7tech.policy.assertion.Assertion;

/**
 * Configure new Non-SOAP Encrypt Element assertion instances when first added to a policy.
 */
public class NonSoapEncryptElementAdvice extends DefaultAssertionAdvice<NonSoapEncryptElementAssertion> {
    @Override
    public void proceed(PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();

        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof NonSoapEncryptElementAssertion)) {
            throw new IllegalArgumentException("Attempting to execute NonSoapEncryptElementAdvice without being given a single NonSoapEncryptElementAssertion");
        }

        NonSoapEncryptElementAssertion subject = (NonSoapEncryptElementAssertion) assertions[0];

        // Configure new assertions to use OAEP by default (SSG-7462)
        subject.setUseOaep(true);

        super.proceed(pc);
    }
}
