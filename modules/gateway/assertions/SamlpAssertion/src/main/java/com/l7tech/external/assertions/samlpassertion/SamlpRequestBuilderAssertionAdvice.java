package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.DefaultAssertionAdvice;
import com.l7tech.policy.assertion.Assertion;

/**
 * Configures new SAMLP request builder assertions when dragged into the policy.
 */
public class SamlpRequestBuilderAssertionAdvice extends DefaultAssertionAdvice<SamlpRequestBuilderAssertion> {
    @Override
    public void proceed(PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();

        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof SamlpRequestBuilderAssertion)) {
            throw new IllegalArgumentException("Attempting to execute SamlpRequestBuilderAssertionAdvice without being given a single SamlpRequestBuilderAssertion");
        }

        SamlpRequestBuilderAssertion subject = (SamlpRequestBuilderAssertion) assertions[0];

        // Configure new assertions to use OAEP by default any time encryption is enabled (SSG-7462)
        subject.getXmlEncryptConfig().setUseOaep(true);

        super.proceed(pc);
    }
}
