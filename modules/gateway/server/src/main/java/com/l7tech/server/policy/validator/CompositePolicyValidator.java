package com.l7tech.server.policy.validator;

import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.PolicyValidationContext;

import java.util.Collections;
import java.util.List;

/**
 * Policy validator that combines the results of multiple underlying validators.
 */
public class CompositePolicyValidator implements PolicyValidator {

    //- PUBLIC

    public CompositePolicyValidator( final List<PolicyValidator> validators ) {
        this.validators = Collections.unmodifiableList( validators );
    }

    @Override
    public PolicyValidatorResult validate( final Assertion assertion,
                                           final PolicyValidationContext pvc,
                                           final AssertionLicense assertionLicense ) throws InterruptedException {
        final PolicyValidatorResult result = new PolicyValidatorResult();

        for ( final PolicyValidator validator : validators ) {
            final PolicyValidatorResult validatorResult =
                    validator.validate( assertion, pvc, assertionLicense );

            for ( final PolicyValidatorResult.Warning message : validatorResult.getWarnings() ) {
                result.addWarning( message );
            }

            for ( final PolicyValidatorResult.Error message : validatorResult.getErrors() ) {
                result.addError( message );
            }
        }

        return result;
    }

    @Override
    public void checkForCircularIncludes( final String policyId,
                                          final String policyName,
                                          final Assertion rootAssertion,
                                          final PolicyValidatorResult r ) {
        // we'll assume one circular include check is as good as another.
        if ( !validators.isEmpty() ) {
            validators.get( 0 ).checkForCircularIncludes( policyId, policyName, rootAssertion, r );
        }
    }

    //- PRIVATE

    private final List<PolicyValidator> validators;
}
