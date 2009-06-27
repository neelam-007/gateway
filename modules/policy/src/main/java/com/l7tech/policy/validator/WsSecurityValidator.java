package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.policy.assertion.xmlsec.AddWssUsernameToken;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.wsdl.Wsdl;

/**
 * Policy validator for WsSecurity assertion.
 */
public class WsSecurityValidator implements AssertionValidator {

    //- PUBLIC

    public WsSecurityValidator( final WsSecurity wsSecurity ) {
        this.wsSecurity = wsSecurity;
        initValidationMessages();
    }

    @Override
    public void validate( final AssertionPath path,
                          final Wsdl wsdl,
                          final boolean soap,
                          final PolicyValidatorResult result ) {
        if ( errString != null && hasDefaultActorEncryption(path.getPath()) ) {
            result.addError(new PolicyValidatorResult.Error(wsSecurity, path, errString, null));
        }
    }

    //- PRIVATE

    private final WsSecurity wsSecurity;
    private String errString;

    private void initValidationMessages() {
        if ( !Assertion.isResponse( wsSecurity ) &&
             wsSecurity.isApplyWsSecurity() && 
             wsSecurity.getRecipientTrustedCertificateName() == null &&
             wsSecurity.getRecipientTrustedCertificateOid() == 0 ) {
            errString = "A \"default recipient certificate\" must be selected for encryption. This assertion will always fail.";
        }
    }

    private boolean hasDefaultActorEncryption( final Assertion[] path ) {
        boolean found = false;

        for ( Assertion assertion : path ) {
            if ( !assertion.isEnabled() ) continue;
            if ( assertion == wsSecurity ) {
                break;
            }
            if ( ( assertion instanceof WssEncryptElement ||
                   (assertion instanceof AddWssUsernameToken && ((AddWssUsernameToken)assertion).isEncrypt() ) ) &&
                 AssertionUtils.isSameTargetMessage(wsSecurity, assertion) &&
                 ((SecurityHeaderAddressable)assertion).getRecipientContext().localRecipient() ) {
                found = true;
            }
        }

        return found;
    }
}