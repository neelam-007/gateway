package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.WsiBspAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.wsdl.Wsdl;

/**
 * Assertion validator for WS-I Basic Security Profile compliance.
 *
 * <p>This validator differs from others in that it places additional contraints
 * on other assertions rather than validating its own configuration.</p>
 */
public class WsiBspAssertionValidator implements AssertionValidator {

    //- PUBLIC

    /**
     *
     */
    public WsiBspAssertionValidator(WsiBspAssertion assertion) {
    }

    /**
     *
     */
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if(path!=null && result!=null) {
            int count = path.getPathCount();
            for(int p=0; p<count; p++) {
                Assertion assertion = path.getPathAssertion(p);

                if(assertion instanceof RequireWssEncryptedElement) {
                    checkAssertion((RequireWssEncryptedElement)assertion, path, result);
                }
                else if(assertion instanceof WssEncryptElement) {
                    checkAssertion((WssEncryptElement)assertion, path, result);
                }
            }
        }
    }

    //- PRIVATE

    private void checkAssertion(RequireWssEncryptedElement assertion, AssertionPath path, PolicyValidatorResult result) {
        if(!permittedEncryptionAlgorithm(assertion.getXEncAlgorithm())) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                    "Encryption is not WS-I Basic Security Profile compliant.", null));
        }
    }

    private void checkAssertion(WssEncryptElement assertion, AssertionPath path, PolicyValidatorResult result) {
        if(!permittedEncryptionAlgorithm(assertion.getXEncAlgorithm())) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                    "Encryption is not WS-I Basic Security Profile compliant.", null));
        }
    }

    private boolean permittedEncryptionAlgorithm(String algorithm) {
        boolean valid = false;

        if(XencUtil.AES_128_CBC.equals(algorithm)
        || XencUtil.AES_256_CBC.equals(algorithm)
        || XencUtil.TRIPLE_DES_CBC.equals(algorithm)) {
            valid = true;
        }

        return valid;
    }
}
