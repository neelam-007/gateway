package com.l7tech.external.assertions.validatenonsoapsaml;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;

/**
 * AssertionValidator for ValidateNonSoapSamlTokenAssertion.
 */
public class ValidateNonSoapSamlTokenAssertionValidator implements AssertionValidator {
    public ValidateNonSoapSamlTokenAssertionValidator(final ValidateNonSoapSamlTokenAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public void validate(final AssertionPath path, final PolicyValidationContext pvc, final PolicyValidatorResult result) {
        if (!assertion.isRequireDigitalSignature()) {
            result.addWarning((new PolicyValidatorResult.Warning(assertion, NO_SIGNATURE, null)));
        }
    }

    static final String NO_SIGNATURE = "No signature is required. Token issuer cannot be verified";
    private ValidateNonSoapSamlTokenAssertion assertion;
}
