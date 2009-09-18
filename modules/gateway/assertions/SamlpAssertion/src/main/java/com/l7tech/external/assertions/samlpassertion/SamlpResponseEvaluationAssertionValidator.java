package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.wsdl.Wsdl;

/**
 * User: vchan
 */
public class SamlpResponseEvaluationAssertionValidator implements AssertionValidator {

    private final SamlpResponseEvaluationAssertion assertion;

    public SamlpResponseEvaluationAssertionValidator(SamlpResponseEvaluationAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {

        if (assertion != null) {
        }
    }
}