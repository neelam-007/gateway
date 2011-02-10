package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.security.xml.WsSecurityVersion;

/**
 * AssertionValidator for the WssConfiguration assertion
 */
public class WssConfigurationAssertionValidator extends WssDecorationAssertionValidator {

    public WssConfigurationAssertionValidator( final WssConfigurationAssertion assertion ) {
        super( assertion, assertion.getWssVersion()==WsSecurityVersion.WSS11, requiresDecoration(assertion) );
    }

    private static boolean requiresDecoration( final WssConfigurationAssertion assertion ) {
        return  !Assertion.isResponse( assertion ) &&
                (assertion.isAddTimestamp() ||
                 assertion.isSignTimestamp() ||
                 assertion.isSignWsAddressingHeaders());
    }
}
