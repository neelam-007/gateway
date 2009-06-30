package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.xmlsec.AddWssUsernameToken;

/**
 * Policy validation for Add WS-Security UsernameToken assertion
 */
public class AddWssUsernameTokenValidator extends WssEncryptingDecorationAssertionValidator {

    public AddWssUsernameTokenValidator( final AddWssUsernameToken assertion ) {
        super( assertion, assertion.isEncrypt(), assertion.isEncrypt() );
    }
}
