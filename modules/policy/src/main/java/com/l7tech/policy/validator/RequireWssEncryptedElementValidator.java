package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement;

/**
 *
 */
public class RequireWssEncryptedElementValidator extends XpathBasedAssertionValidator {

    //- PUBLIC

    public RequireWssEncryptedElementValidator( final RequireWssEncryptedElement assertion ) {
        super(assertion);
        this.permittedXencAlgorithmListValidator = new HasPermittedXencAlgorithmListValidator<RequireWssEncryptedElement>(assertion);
    }

    @Override
    public void validate( final AssertionPath path,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {
        super.validate( path, pvc, result );
        permittedXencAlgorithmListValidator.validate( path, pvc, result );
    }

    //- PRIVATE

    private final HasPermittedXencAlgorithmListValidator<RequireWssEncryptedElement> permittedXencAlgorithmListValidator;
}
