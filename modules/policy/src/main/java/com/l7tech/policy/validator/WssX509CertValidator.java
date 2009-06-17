package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.wsdl.Wsdl;

/**
 * Assertion validator for WSS X.509 Signature assertion.
 *
 * <p>This validator warns if multiple signatures are permitted.</p>
 *
 * <p>The validator also warns of any IdentityTargetable assertions that do
 * not specify an identity if multiple authentication is enabled for the
 * target WSS X.509 Signature assertion.</p>
 */
public class WssX509CertValidator implements AssertionValidator {

    //- PUBLIC

    /**
     *
     */
    public WssX509CertValidator( final RequireWssX509Cert assertion ) {
        this.assertion = assertion;
    }

    /**
     *
     */
    @Override
    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        if ( assertion.isAllowMultipleSignatures() ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                    "Multiple signatures are permitted. This is an advanced setting and should be used with caution.", null));

            for ( Assertion pathAssertion : path.getPath() ) {
                if ( pathAssertion instanceof IdentityTargetable &&
                     AssertionUtils.isSameTargetMessage( assertion, pathAssertion ) &&
                     new IdentityTarget().equals( new IdentityTarget(((IdentityTargetable) pathAssertion).getIdentityTarget()) ))  {
                    result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, path,
                            "Multiple signatures are permitted, this assertion must specify a target identity.", null));
                }
            }
            
        }
    }

    //- PRIVATE

    private final RequireWssX509Cert assertion;
}
