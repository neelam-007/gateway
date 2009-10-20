package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.FaultLevel;

/**
 * Assertion validator for SOAP Fault Level assertion.
 */
public class FaultLevelAssertionValidator extends AssertionValidatorSupport<FaultLevel> {

    public FaultLevelAssertionValidator( final FaultLevel assertion ) {
        super(assertion);
        if ( assertion.isEnabled() && !assertion.isUsesDefaultKeyStore() && !assertion.getLevelInfo().isSignSoapFault() ) {
            this.addWarningMessage( "The selected private key will be ignored, signing is not enabled." );   
        }
    }
}
