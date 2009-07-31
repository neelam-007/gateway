package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.IdentityTagable;
import com.l7tech.wsdl.Wsdl;

/**
 * Policy validator for IdentityTagable assertions.
 */
public class IdentityTagAssertionValidator implements AssertionValidator {

    //- PUBLIC

    public IdentityTagAssertionValidator( final Assertion assertion ) {
        this.assertion = assertion;
        this.tag = getIdentityTag(assertion);
    }

    @Override
    public void validate( final AssertionPath assertionPath,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        if ( tag != null ) {

            for ( Assertion pathAssertion : assertionPath.getPath() ) {
                if ( pathAssertion == assertion ) {
                    break;
                } else if (!pathAssertion.isEnabled()) {
                    continue;
                }

                if ( tag.equalsIgnoreCase( getIdentityTag(pathAssertion) ) &&
                     AssertionUtils.isSameTargetMessage( assertion, pathAssertion )) {
                    result.addWarning( new PolicyValidatorResult.Warning( assertion, assertionPath, "Assertion uses the same Identity Tag as another assertion in the same path.", null ) );
                    break;
                }
            }
        }
    }

    //- PRIVATE

    private final Assertion assertion;
    private final String tag;

    private String getIdentityTag( final Assertion assertion ) {
        String tag = null;

        if ( assertion instanceof IdentityTagable ) {
            IdentityTagable identityTagable = (IdentityTagable) assertion;
            tag = identityTagable.getIdentityTag();
        }

        return tag;
    }
}