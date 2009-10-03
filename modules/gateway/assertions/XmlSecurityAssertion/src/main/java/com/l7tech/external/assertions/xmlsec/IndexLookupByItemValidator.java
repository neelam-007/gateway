package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.validator.AssertionValidatorSupport;

/**
 *
 */
public class IndexLookupByItemValidator extends AssertionValidatorSupport<IndexLookupByItemAssertion> {
    public IndexLookupByItemValidator(IndexLookupByItemAssertion assertion) {
        super(assertion);
        requireNonEmpty(assertion.getMultivaluedVariableName(),
                "No multivalued variable name configured.  Assertion will always fail.");
        requireNonEmpty(assertion.getOutputVariableName(),
                "No output variable name configured.  Assertion will always fail.");
        requireNonEmpty(assertion.getValueToSearchForVariableName(),
                "No value to search for configured.  Assertion will always fail.");
    }
}
