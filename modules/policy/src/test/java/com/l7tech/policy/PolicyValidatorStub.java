package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.PolicyValidationContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class PolicyValidatorStub implements PolicyValidator {

    //- PUBLIC

    @Override
    public PolicyValidatorResult validate( final @Nullable Assertion assertion,
                                           final PolicyValidationContext pvc,
                                           final AssertionLicense assertionLicense) throws InterruptedException {
        final PolicyValidatorResult result = new PolicyValidatorResult();

        for ( PolicyValidatorResult.Warning warning : warnings ) {
            result.addWarning( warning );
        }

        for ( PolicyValidatorResult.Error error : errors ) {
            result.addError( error );
        }

        return result;
    }

    @Override
    public void checkForCircularIncludes( final String policyId,
                                          final String policyName,
                                          final @Nullable Assertion rootAssertion,
                                          final PolicyValidatorResult r ) {
    }

    public void setWarnings( final Collection<PolicyValidatorResult.Warning> warnings ) {
        this.warnings.clear();
        this.warnings.addAll( warnings );
    }

    public void setErrors( final Collection<PolicyValidatorResult.Error> errors ) {
        this.errors.clear();
        this.errors.addAll( errors );
    }

    //- PRIVATE

    private final Collection<PolicyValidatorResult.Warning> warnings = new ArrayList<PolicyValidatorResult.Warning>();
    private final Collection<PolicyValidatorResult.Error> errors = new ArrayList<PolicyValidatorResult.Error>();
}
