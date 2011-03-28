package com.l7tech.external.assertions.wsaddressing;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;

/**
 *
 */
public class WsAddressingAssertionValidator implements AssertionValidator {
    final WsAddressingAssertion assertion;
    private boolean nonLocalSignature;

    public WsAddressingAssertionValidator(WsAddressingAssertion assertion) {
        this.assertion = assertion;
        this.nonLocalSignature = assertion.isRequireSignature() && !assertion.getRecipientContext().localRecipient();
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        if (nonLocalSignature)
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
                    "This assertion requires a signature, but has a WSSRecipient other than \"Default\".  This assertion will never succeed.", null));
    }
}
