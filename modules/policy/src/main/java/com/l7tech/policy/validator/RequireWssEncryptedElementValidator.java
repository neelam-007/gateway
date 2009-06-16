package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement;
import com.l7tech.wsdl.Wsdl;

import java.util.*;

/**
 *
 */
public class RequireWssEncryptedElementValidator implements AssertionValidator {

    //- PUBLIC

    public RequireWssEncryptedElementValidator( final RequireWssEncryptedElement assertion ) {
        this.assertion = assertion;
    }

    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        Assertion[] assertionPath = path.getPath();
        for ( int i = assertionPath.length - 1; i >= 0; i-- ) {
            Assertion a = assertionPath[i];
            if ( a != assertion && a instanceof RequireWssEncryptedElement ) {
                RequireWssEncryptedElement requireWssEncryptedElement = (RequireWssEncryptedElement)a;
                if ( AssertionUtils.isSameTargetMessage( assertion, a ) &&
                     !new HashSet<String>(requireWssEncryptedElement.getXEncAlgorithmList()).equals(new HashSet<String>(assertion.getXEncAlgorithmList())) ) {
                    String message = "Multiple encryption assertions are present with different encryption algorithms.";
                    result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                }
            }
        }
    }

    //- PRIVATE

    private final RequireWssEncryptedElement assertion;
}
