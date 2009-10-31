package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.validator.AssertionValidatorSupport;

/**
 *
 */
public class VariableCredentialSourceValidator extends AssertionValidatorSupport<VariableCredentialSourceAssertion> {
    public VariableCredentialSourceValidator(VariableCredentialSourceAssertion assertion) {
        super(assertion);
        requireNonEmpty(assertion.getVariableName(),
                "No variable name is specified.  Assertion will always fail.");
    }
}
