package com.l7tech.external.assertions.certificateattributes;

import com.l7tech.policy.validator.AbstractPolicyValidator;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.wsdl.Wsdl;

/**
 * Validator for CertificateAttributesAssertion.
 */
public class CertificateAttributesAssertionValidator implements AssertionValidator {
    private final CertificateAttributesAssertion assertion;

    public CertificateAttributesAssertionValidator(CertificateAttributesAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        int firstCertCred = -1;
        int firstIdentity = -1;
        for (int i = 0; i < path.getPath().length; i++) {
            Assertion ass = path.getPath()[i];
            if (!ass.isEnabled()) continue;
            if (ass.isCredentialSource()) {
                if (AbstractPolicyValidator.isX509CredentialSource(ass)) {
                    firstCertCred = i;
                }
            } else if (ass instanceof IdentityAssertion) {
                firstIdentity = i;
            } else if (ass == assertion) {
                if (firstCertCred == -1 || firstCertCred > i)
                    result.addError(new PolicyValidatorResult.Error(assertion, path, "Must be preceded by a certificate credential source", null));
                if (firstIdentity == -1 || firstIdentity > i)
                    result.addError(new PolicyValidatorResult.Error(assertion, path, "Must be preceded by an identity assertion (e.g. Authenticate User or Group)", null));
                return;
            }
        }
    }
}
