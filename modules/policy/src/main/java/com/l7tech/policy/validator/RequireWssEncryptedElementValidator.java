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
public class RequireWssEncryptedElementValidator extends XpathBasedAssertionValidator {

    //- PUBLIC

    public RequireWssEncryptedElementValidator( final RequireWssEncryptedElement assertion ) {
        super(assertion);
        this.assertion = assertion;
    }

    @Override
    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        super.validate( path, wsdl, soap, result );

        Assertion[] assertionPath = path.getPath();
        for ( int i = assertionPath.length - 1; i >= 0; i-- ) {
            Assertion a = assertionPath[i];
            if (!a.isEnabled()) continue;
            if ( a != assertion &&
                 a instanceof RequireWssEncryptedElement &&
                 AssertionUtils.isSameTargetRecipient( assertion, a ) &&
                 AssertionUtils.isSameTargetMessage( assertion, a ) ) {
                RequireWssEncryptedElement requireWssEncryptedElement = (RequireWssEncryptedElement)a;
                if ( !getXEncAlgorithms(requireWssEncryptedElement).equals(getXEncAlgorithms(assertion)) ) {
                    String message = "Multiple encryption assertions are present with different encryption algorithms.";
                    result.addError(new PolicyValidatorResult.Error(assertion, path, message, null));
                }
            }
        }
    }

    //- PRIVATE

    private final RequireWssEncryptedElement assertion;

    private Set<String> getXEncAlgorithms( final RequireWssEncryptedElement assertion ) {
        Set<String> algorithms = new HashSet<String>();

        if ( assertion.getXEncAlgorithmList() != null ) {
            algorithms.addAll( assertion.getXEncAlgorithmList() );
        } else {
            algorithms.add( assertion.getXEncAlgorithm() );   
        }

        return algorithms;
    }
}
