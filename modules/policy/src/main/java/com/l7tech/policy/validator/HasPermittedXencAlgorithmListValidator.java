package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.HasPermittedXencAlgorithmList;

import java.util.List;

/**
 * Validator that checks for multiple HasPermittedXencAlgorithmList in the same policy path with conflicting
 * algorithm requirements.
 */
public class HasPermittedXencAlgorithmListValidator<AT extends Assertion&HasPermittedXencAlgorithmList> implements AssertionValidator {

    //- PUBLIC

    public HasPermittedXencAlgorithmListValidator( final AT assertion ) {
        this.assertion = assertion;
    }

    @Override
    public void validate( final AssertionPath path,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {

        Assertion[] assertionPath = path.getPath();
        for ( int i = assertionPath.length - 1; i >= 0; i-- ) {
            Assertion a = assertionPath[i];
            if (!a.isEnabled()) continue;
            if ( a != assertion &&
                 a instanceof HasPermittedXencAlgorithmList &&
                 AssertionUtils.isSameTargetRecipient(assertion, a) &&
                 AssertionUtils.isSameTargetMessage( assertion, a ) ) {
                HasPermittedXencAlgorithmList hasAlgs = (HasPermittedXencAlgorithmList)a;
                if ( !isSameXEncAlgorithms(hasAlgs.getXEncAlgorithmList(), assertion.getXEncAlgorithmList()) ) {
                    String message = "Multiple encryption assertions are present with different encryption algorithms.";
                    result.addError(new PolicyValidatorResult.Error(assertion, message, null));
                }
            }
        }
    }

    //- PRIVATE

    private final AT assertion;

    private boolean isSameXEncAlgorithms(List<String> a, List<String> b) {
        // Alg pref lists are optional, but if both specified, they must both have the same algorithms in the same order.
        return a == null || b == null || a.equals(b);
    }
}
