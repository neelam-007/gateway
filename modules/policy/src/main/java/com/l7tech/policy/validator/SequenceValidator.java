package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * AssertionValidator that delegates to a List of AssertionValidators
 *
 * @author steve
 */
public class SequenceValidator implements AssertionValidator {

    //- PUBLIC
    
    @Override
    public void validate( final AssertionPath path, final PolicyValidationContext pvc, final PolicyValidatorResult result ) {
        for ( AssertionValidator validator : validators ) {
            validator.validate( path, pvc, result );
        }
    }

    //- PACKAGE

    SequenceValidator( final AssertionValidator validatorA, final AssertionValidator validatorB ) {
        this.validators = new ArrayList<AssertionValidator>();
        this.validators.add( validatorA );
        this.validators.add( validatorB );
    }

    SequenceValidator( final Collection<AssertionValidator> validators ) {
        this.validators = new ArrayList<AssertionValidator>(validators);
    }

    //- PRIVATE

    private final List<AssertionValidator> validators;
}
