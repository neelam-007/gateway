package com.l7tech.external.assertions.certificateattributes;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
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

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        int firstCertCred = -1;
        int firstIdentity = -1;
        for (int i = 0; i < path.getPath().length; i++) {
            Assertion ass = path.getPath()[i];
            if (!ass.isEnabled()) continue;
            if (ass.isCredentialSource()) {
                if (ass instanceof RequireWssX509Cert || ass instanceof SslAssertion || ass instanceof SecureConversation) {
                    firstCertCred = i;
                } else if (ass instanceof RequireWssSaml) {
                    RequireWssSaml saml = (RequireWssSaml) ass;
                    final String[] scs = saml.getSubjectConfirmations();
                    // RequestWssSaml is only a cert-based credential source if it's Holder of Key and has the signature constraint
                    if (scs.length == 1 && SamlIssuerAssertion.HOK_URIS.contains(scs[0]) && saml.isRequireHolderOfKeyWithMessageSignature()) {
                        firstCertCred = i;
                    }
                }
            } else if (ass instanceof IdentityAssertion) {
                firstIdentity = i;
            } else if (ass == assertion) {
                if (firstCertCred == -1 || firstCertCred > i)
                    result.addError(new PolicyValidatorResult.Error(assertion, path, "Must be preceded by a certificate credential source", null));
                if (firstIdentity == -1 || firstIdentity > i)
                    result.addError(new PolicyValidatorResult.Error(assertion, path, "Must be preceded by an identity assertion (either Specific User or Member Of Group)", null));
                return;
            }
        }
    }
}
