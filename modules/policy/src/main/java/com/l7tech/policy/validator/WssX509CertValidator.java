package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.wsdl.Wsdl;

/**
 * Assertion validator for WSS X.509 Signature assertion.
 *
 * <p>This validator warns if setting the insecure variable for certificates
 * found in the message.</p>
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
    public WssX509CertValidator( final RequestWssX509Cert assertion ) {
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
            TargetMessageType targetMessageType = assertion.getTarget();

            for ( Assertion pathAssertion : path.getPath() ) {
                if ( pathAssertion instanceof IdentityTargetable &&
                     targetMessageType == Assertion.getTargetMessageType(pathAssertion) &&
                     new IdentityTarget().equals( new IdentityTarget(((IdentityTargetable) pathAssertion).getIdentityTarget()) ))  {
                    result.addWarning(new PolicyValidatorResult.Warning(pathAssertion, path,
                            "Assertions Target Identity should be selected when multiple identities are in use.", null));
                }
            }
            
        }
    }

    //- PRIVATE

    private final RequestWssX509Cert assertion;
}
