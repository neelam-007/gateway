package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.wsdl.Wsdl;

/**
 * User: vchan
 */
public class SamlpRequestBuilderAssertionValidator implements AssertionValidator {

    private final SamlpRequestBuilderAssertion assertion;
    private final boolean holderOfKey;
    private final boolean isNameIdSpecified;

    public SamlpRequestBuilderAssertionValidator(SamlpRequestBuilderAssertion assertion) {
        this.assertion = assertion;
        this.holderOfKey = SamlpRequestBuilderAssertion.HOK_URIS.contains(assertion.getSubjectConfirmationMethodUri());
        this.isNameIdSpecified = NameIdentifierInclusionType.SPECIFIED.equals(assertion.getNameIdentifierType());
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {

        if (assertion != null) {
            int firstCreds = -1;
            int firstCertCred = -1;
            int firstRoute = -1;
            for (int i = 0; i < path.getPath().length; i++) {
                Assertion ass = path.getPath()[i];
                if (ass instanceof RoutingAssertion) {
                    firstRoute = i;
                } else if (ass.isCredentialSource()) {
                    firstCreds = i;
                    if (ass instanceof RequestWssX509Cert || ass instanceof SslAssertion || ass instanceof SecureConversation) {
                        firstCertCred = i;
                    }
                } else if (ass == assertion) {
                    if (!(holderOfKey || isNameIdSpecified) && (firstCreds == -1 || firstCreds > i)) {
                        result.addError(new PolicyValidatorResult.Error(assertion, path, "Must be preceded by a credential source", null));
                    }

                    if (holderOfKey && firstCertCred == -1 || firstCertCred > i) {
                        result.addError(new PolicyValidatorResult.Error(assertion, path, "Holder-of-Key selected, must be preceded by a certificate-based credential source", null));
                    }

                    return;
                }
            }
        }
    }
}
