package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.xmlsec.*;

/**
 * AssertionValidator for assertions that perform encryption with WsSecurity assertion.
 */
abstract class WssEncryptingDecorationAssertionValidator extends WssDecorationAssertionValidator {

    //- PUBLIC

    public WssEncryptingDecorationAssertionValidator( final Assertion assertion,
                                                      boolean encrypts,
                                                      boolean isWss11 ) {
        super(assertion, isWss11);
        this.encrypts = encrypts;
        this.defaultActor = isDefaultActor( assertion );
        this.defaultIdentityTarget = isDefaultIdentityTarget( assertion );
    }

    @Override
    public void validate( final AssertionPath path,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {
        super.validate( path, pvc, result );

        if ( (!defaultActor || !Assertion.isResponse(assertion )) && !defaultIdentityTarget ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
                    "The \"Target Identity\" will be ignored for this assertion. A \"Target Identity\" should be used for response encryption when multiple identities are used in a request.", null));
        }

        boolean seenSelf = true;
        boolean requiresTargetEncKey = encrypts && defaultActor && Assertion.isResponse(assertion); // non response validation is handled by WsSecurity validator
        Assertion[] assertionPath = path.getPath();
        for (int i = assertionPath.length - 1; i >= 0; i--) {
            Assertion a = assertionPath[i];
            if (!a.isEnabled()) continue;

            if ( a == assertion ) {
                seenSelf = false; // loop is backwards
            } else {
                // Callback to validate each assertion in path
                validateAssertion( path, a, result );
            }

            if ( requiresTargetEncKey &&
                 (a instanceof RequestWssKerberos ||
                  a instanceof EncryptedUsernameTokenAssertion ||
                  a instanceof RequireWssSaml ||
                  a instanceof SecureConversation ||
                  a instanceof RequireWssX509Cert ) &&
                 isDefaultActor(a) &&
                 Assertion.isRequest( a )  ) {
                requiresTargetEncKey = false;
            }
            if ( seenSelf && a instanceof WsSecurity && AssertionUtils.isSameTargetMessage( assertion, a ) ) {
                WsSecurity wsSecurity = (WsSecurity) a;
                if ( wsSecurity.isApplyWsSecurity() &&
                     ( wsSecurity.getRecipientTrustedCertificateName() != null ||
                       wsSecurity.getRecipientTrustedCertificateOid() != null ) ) {
                        requiresTargetEncKey = false;
                }
            }

        }

        if ( requiresTargetEncKey ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
                    "This assertion requires a (Request) Require WS-Security Signature Credentials assertion, a Require WS-SecureConversation assertion, a Require SAML Token Profile Credentials assertion, a Require Encrypted UsernameToken Profile Credentials assertion, or a Require WS-Security Kerberos Token Profile Credentials.", null));
        }
    }

    //- PROTECTED

    /**
     * Override to perform validation for each assertion in the path (in reverse order)  
     */
    protected void validateAssertion( final AssertionPath path,  final Assertion a, PolicyValidatorResult result ) {                
    }

    //- PRIVATE

    private boolean defaultActor;
    private boolean defaultIdentityTarget;
    private boolean encrypts;

    private static boolean isDefaultActor( final Assertion assertion ) {
        boolean defaultActor = true;

        if ( assertion instanceof SecurityHeaderAddressable ) {
            SecurityHeaderAddressable securityHeaderAddressable = (SecurityHeaderAddressable) assertion;
            defaultActor = securityHeaderAddressable.getRecipientContext().localRecipient();
        }

        return defaultActor;
    }

    private static boolean isDefaultIdentityTarget( final Assertion assertion ) {
        boolean defaultIdentityTarget = true;

        if ( assertion instanceof IdentityTargetable ) {
            defaultIdentityTarget = new IdentityTarget().equals(new IdentityTarget(((IdentityTargetable)assertion).getIdentityTarget()));
        }

        return defaultIdentityTarget;
    }
}
