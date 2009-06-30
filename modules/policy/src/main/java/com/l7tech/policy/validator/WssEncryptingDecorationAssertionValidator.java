package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.wsdl.Wsdl;

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
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        super.validate( path, wsdl, soap, result );

        if ( !defaultActor || !Assertion.isResponse(assertion ) && !defaultIdentityTarget ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
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
                       wsSecurity.getRecipientTrustedCertificateOid() > 0 ) ) {
                        requiresTargetEncKey = false;
                }
            }

        }

        if ( requiresTargetEncKey ) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                    "This assertion requires a (Request) WSS Signature assertion, a WS Secure Conversation assertion, a SAML assertion, an Encrypted UsernameToken assertion, or a WSS Kerberos assertion.", null));
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
