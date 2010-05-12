package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.XpathBasedAssertion;

/**
 * A composite validator that performs validations for both {@link XpathBasedAssertionValidator} and
 * {@link ElementSelectingXpathValidator}.
 */
public class ElementSelectingXpathBasedAssertionValidator extends XpathBasedAssertionValidator {
    private final AssertionValidator elementSelectingXpathValidator;

    public ElementSelectingXpathBasedAssertionValidator(final XpathBasedAssertion xpathBasedAssertion) {
        super(xpathBasedAssertion);
        elementSelectingXpathValidator = new ElementSelectingXpathValidator(xpathBasedAssertion);
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate(path, pvc, result);
        elementSelectingXpathValidator.validate(path, pvc, result);
    }
}
