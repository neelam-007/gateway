package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.validator.AssertionValidatorSupport;

/**
 *
 */
public class ItemLookupByIndexValidator extends AssertionValidatorSupport<ItemLookupByIndexAssertion> {
    public ItemLookupByIndexValidator(ItemLookupByIndexAssertion assertion) {
        super(assertion);
        requireNonEmpty(assertion.getMultivaluedVariableName(),
                "No multivalued variable name configured.  Assertion will always fail.");
        requireNonEmpty(assertion.getOutputVariableName(),
                "No output variable name configured.  Assertion will always fail.");
        requireValidIntegerOrContextVariable(assertion.getIndexValue(),
                "No index value configured.  Assertion will always fail.",
                "Index value must either be a number or an interpolated ${variable} that will expand to a number.  Assertion will always fail.");
    }
}
