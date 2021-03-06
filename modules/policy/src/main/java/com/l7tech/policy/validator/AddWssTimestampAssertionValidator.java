package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;

/**
 * Assertion validator for AddWssTimestamp
 */
public class AddWssTimestampAssertionValidator extends WssDecorationAssertionValidator {

    //- PUBLIC

    public AddWssTimestampAssertionValidator( final AddWssTimestamp assertion ) {
        super( assertion );
        this.assertion = assertion;
        this.warningMessage = getWarning( assertion );
    }

    @Override
    public void validate( final AssertionPath path,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {
        if ( warningMessage != null ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, warningMessage, null));
        }

        super.validate( path, pvc, result );
    }

    //- PRIVATE

    private final AddWssTimestamp assertion;
    private final String warningMessage;

    private String getWarning( final AddWssTimestamp assertion ) {
        String warning = null;

        if ( !assertion.isSignatureRequired() && !assertion.isUsesDefaultKeyStore() ) {
            warning = "The selected Private Key will be ignored as this assertion is not configured to sign the timestamp.";               
        }

        return warning;
    }
}
