package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.security.xml.WsSecurityVersion;

/**
 * AssertionValidator for the WssConfiguration assertion
 */
public class WssConfigurationAssertionValidator extends WssDecorationAssertionValidator {

    public WssConfigurationAssertionValidator( final WssConfigurationAssertion assertion ) {
        super( assertion, assertion.getWssVersion()==WsSecurityVersion.WSS11, requiresDecoration(assertion) );
        this.versionValidator = new WssVersionAssertionValidator( assertion );
    }

    @Override
    public void validate( final AssertionPath path, final PolicyValidationContext pvc, final PolicyValidatorResult result ) {
        super.validate( path, pvc, result );
        versionValidator.validate( path, pvc, result );
    }

    //- PRIVATE

    private final WssVersionAssertionValidator versionValidator;

    private static boolean requiresDecoration( final WssConfigurationAssertion assertion ) {
        return  !Assertion.isResponse( assertion ) &&
                (assertion.isAddTimestamp() ||
                 assertion.isSignTimestamp() ||
                 assertion.isSignWsAddressingHeaders());
    }
}
