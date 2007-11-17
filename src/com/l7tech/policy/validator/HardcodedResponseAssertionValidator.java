package com.l7tech.policy.validator;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;

import java.io.IOException;

/**
 * AssertionValidator for HardcodedResponseAssertion.
 */
public class HardcodedResponseAssertionValidator implements AssertionValidator {
    private final HardcodedResponseAssertion ass;
    private Throwable e;

    public HardcodedResponseAssertionValidator(HardcodedResponseAssertion ass) {
        this.ass = ass;
        try {
            ContentTypeHeader.parseValue(ass.getResponseContentType());
        } catch (IOException e) {
            this.e = e;
        }
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (e != null)
            result.addError(new PolicyValidatorResult.Error(ass, path, "the content type is invalid. " + e.getMessage(), null));
    }
}
