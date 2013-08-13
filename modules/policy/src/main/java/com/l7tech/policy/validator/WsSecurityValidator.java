package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;
import com.l7tech.policy.assertion.xmlsec.AddWssUsernameToken;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.security.token.SecurityTokenType;

/**
 * Policy validator for WsSecurity assertion.
 */
public class WsSecurityValidator implements AssertionValidator {

    //- PUBLIC

    public WsSecurityValidator( final WsSecurity wsSecurity ) {
        this.wsSecurity = wsSecurity;
        this.versionValidator = new WssVersionAssertionValidator( wsSecurity );
        initValidationMessages();
    }

    @Override
    public void validate( final AssertionPath path,
                          final PolicyValidationContext pvc,
                          final PolicyValidatorResult result ) {
        if ( errString != null && hasDefaultActorEncryption(path.getPath()) && !hasEncryptionToken(path.getPath())) {
            result.addError(new PolicyValidatorResult.Error(wsSecurity, errString, null));
        }
        versionValidator.validate( path, pvc, result );
    }

    //- PRIVATE

    private final WsSecurity wsSecurity;
    private final WssVersionAssertionValidator versionValidator;
    private String errString;

    private void initValidationMessages() {
        if ( !Assertion.isResponse( wsSecurity ) &&
             wsSecurity.isApplyWsSecurity() && 
             wsSecurity.getRecipientTrustedCertificateName() == null &&
             wsSecurity.getRecipientTrustedCertificateGoid() == null &&
             wsSecurity.getRecipientTrustedCertificateVariable() == null) {
            errString = "A \"default recipient certificate\" must be selected for encryption. This assertion will always fail.";
        }
    }

    private boolean hasDefaultActorEncryption( final Assertion[] path ) {
        boolean found = false;

        for ( final Assertion assertion : path ) {
            if ( !assertion.isEnabled() ) continue;
            if ( assertion == wsSecurity ) {
                break;
            }
            if ( isEncryptionAssertion(assertion) &&
                 AssertionUtils.isSameTargetMessage(wsSecurity, assertion) &&
                 ((SecurityHeaderAddressable)assertion).getRecipientContext().localRecipient() ) {
                found = true;
                break;
            }
        }

        return found;
    }

    private boolean isEncryptionAssertion( final Assertion assertion ) {
        return assertion instanceof WssEncryptElement ||
               (assertion instanceof AddWssUsernameToken && ((AddWssUsernameToken)assertion).isEncrypt()) ||
               (assertion instanceof AddWssSecurityToken && ((AddWssSecurityToken)assertion).isEncrypt() &&
                       ((AddWssSecurityToken)assertion).getTokenType()==SecurityTokenType.WSS_USERNAME) ||
               (assertion instanceof WssConfigurationAssertion && ((WssConfigurationAssertion)assertion).isEncryptSignature() );
    }

    /**
     * Check if there is a token available for use with encryption.
     *
     * <p>Note that an encrypted key still requires another token for the
     * key encryption so this is not treated as an "encryption" token.</p> 
     */
    private boolean hasEncryptionToken( final Assertion[] path ) {
        boolean found = false;

        for ( final Assertion assertion : path ) {
            if ( !assertion.isEnabled() ) continue;
            if ( assertion == wsSecurity ) {
                break;
            }
            if ( assertion instanceof AddWssSecurityToken &&
                 AssertionUtils.isSameTargetMessage(wsSecurity, assertion) &&
                 ((SecurityHeaderAddressable)assertion).getRecipientContext().localRecipient() ) {
                final AddWssSecurityToken addWssSecurityToken = (AddWssSecurityToken) assertion;
                found = addWssSecurityToken.getTokenType() == SecurityTokenType.WSSC_CONTEXT;
                if ( found ) {
                    break;
                }
            }
        }

        return found;
    }
}