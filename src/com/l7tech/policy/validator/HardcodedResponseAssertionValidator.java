package com.l7tech.policy.validator;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.service.PublishedService;

import java.io.IOException;

/**
 * AssertionValidator for HardcodedResponseAssertion.
 */
public class HardcodedResponseAssertionValidator implements AssertionValidator {
    private final HardcodedResponseAssertion ass;

    public HardcodedResponseAssertionValidator(HardcodedResponseAssertion ass) {
        this.ass = ass;
    }

    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
        try {
            ContentTypeHeader.parseValue(ass.getResponseContentType());
        } catch (IOException e) {
            result.addError(new PolicyValidatorResult.Error(ass, path, "the content type is invalid. " + e.getMessage(), null));
        }
    }
}
