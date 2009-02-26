package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.wsdl.Wsdl;

/**
 * This class currently does nothing; the "non-local WSSRecipient" warning it used to log is now
 * logged by the {@link PathValidator} for all {@link com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable} assertions that are targeted
 * at the Request and that do not assert {@link ValidatorFlag#PROCESSES_NON_LOCAL_WSS_RECIPIENT}.
 *
 * @author emil
 * @author vchan
 */
public class RequestWssConfidentialityValidator implements AssertionValidator {
    public RequestWssConfidentialityValidator(RequestWssConfidentiality ra) {
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
    }
}
