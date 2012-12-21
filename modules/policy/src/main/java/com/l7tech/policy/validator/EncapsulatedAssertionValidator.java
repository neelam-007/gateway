package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.EncapsulatedAssertion;

/**
 * Validator for instances of Encapsulated Assertion.
 */
public class EncapsulatedAssertionValidator extends AssertionValidatorSupport<EncapsulatedAssertion> {
    public EncapsulatedAssertionValidator(EncapsulatedAssertion assertion) {
        super(assertion);
        String configGuid = assertion.getEncapsulatedAssertionConfigGuid();
        if (configGuid == null) {
            addMessage("encapsulated assertion does not contain an encapsulated assertion configuration GUID");
        } else {
            // This assumes that design-time entities have already been provided before the validator is invoked
            if (assertion.config() == null)
                addMessage("no encapsulated assertion configuration available with GUID " + configGuid);
        }
    }
}
