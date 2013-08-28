package com.l7tech.external.assertions.siteminder;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;

public class SiteMinderAuthenticateAssertionValidator implements AssertionValidator {

    private SiteMinderAuthenticateAssertion assertion;

    public SiteMinderAuthenticateAssertionValidator(final SiteMinderAuthenticateAssertion a) {
        this.assertion = a;
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        if (!assertion.isUseSMCookie()) {
            int foundCredentialSource = -1;
            Assertion[] assertions = path.getPath();
            for (int i = 0; i < assertions.length; i++) {
                Assertion ass = assertions[i];
                if (ass.isEnabled()) {
                    if (ass == assertion) {
                        if (foundCredentialSource == -1 || foundCredentialSource > i) {
                            result.addError(new PolicyValidatorResult.Error(assertion, "Must be preceded by a credential source", null));
                        }
                        return;
                    } else if (ass.isCredentialSource()) {
                        foundCredentialSource = i;
                    }
                }
            }
        }
    }
}
