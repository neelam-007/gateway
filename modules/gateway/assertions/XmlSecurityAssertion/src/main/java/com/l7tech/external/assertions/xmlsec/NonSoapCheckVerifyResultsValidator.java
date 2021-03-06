package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.*;

/**
 *
 */
public class NonSoapCheckVerifyResultsValidator extends ElementSelectingXpathBasedAssertionValidator {
    private final AssertionValidatorSupport vs;

    public NonSoapCheckVerifyResultsValidator(final NonSoapCheckVerifyResultsAssertion assertion) {
        super(assertion);
        vs = new AssertionValidatorSupport<NonSoapCheckVerifyResultsAssertion>(assertion);
        vs.requireNonEmptyArray(assertion.getPermittedDigestMethodUris(),
                "No digest methods are permitted.  This assertion will always fails.");
        vs.requireNonEmptyArray(assertion.getPermittedSignatureMethodUris(),
                "No signature methods are permitted.  This assertion will always fail.");
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate(path, pvc, result);
        vs.validate(path, pvc, result);
    }
}
