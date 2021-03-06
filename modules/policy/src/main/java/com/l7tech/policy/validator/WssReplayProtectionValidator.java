package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;

/**
 * Policy validator for WssReplayProtection assertions.
 */
public class WssReplayProtectionValidator implements AssertionValidator {

    //- PUBLIC

    public WssReplayProtectionValidator( final WssReplayProtection assertion ) {
        this.assertion = assertion;
    }

    @Override
    public void validate( final AssertionPath assertionPath,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {
        if ( assertion.isCustomProtection() ) {
            if (!SecurityHeaderAddressableSupport.isLocalRecipient(assertion)) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion,
                        "Custom replay protection will be ignored for non-local WSS Recipient.", null));
            }
        } else {
            if ( !pvc.isSoap() ) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion,
                        "Custom replay protection should be used for non SOAP messages.", null));
            }
        }
    }

    //- PRIVATE

    private final WssReplayProtection assertion;
}
