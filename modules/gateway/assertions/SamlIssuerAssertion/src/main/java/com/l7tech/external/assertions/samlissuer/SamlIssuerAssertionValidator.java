/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlissuer;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.security.saml.NameIdentifierInclusionType;

import java.util.EnumSet;

/**
 * @author alex
 */
public class SamlIssuerAssertionValidator implements AssertionValidator {
    private final SamlIssuerAssertion assertion;
    private final boolean holderOfKey;
    private final boolean decorateResponse;
    private final NameIdentifierInclusionType nameIdentifierType;

    public SamlIssuerAssertionValidator(SamlIssuerAssertion sia) {
        this.assertion = sia;
        // Only Holder of Key has validation rules
        final EnumSet<SamlIssuerAssertion.DecorationType> dts = assertion.getDecorationTypes();
        decorateResponse = dts != null && dts.contains(SamlIssuerAssertion.DecorationType.RESPONSE);
        holderOfKey = SamlIssuerAssertion.HOK_URIS.contains(assertion.getSubjectConfirmationMethodUri());
        nameIdentifierType = assertion.getNameIdentifierType();
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        int firstCreds = -1;
        int firstCertCred = -1;
        int firstRoute = -1;
        for (int i = 0; i < path.getPath().length; i++) {
            Assertion ass = path.getPath()[i];
            if (!ass.isEnabled()) continue;
            if (ass instanceof RoutingAssertion) {
                firstRoute = i;
            } else if (ass.isCredentialSource()) {
                firstCreds = i;
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
            } else if (ass == assertion) {
                if ((firstCreds == -1 || firstCreds > i) && !NameIdentifierInclusionType.SPECIFIED.equals(nameIdentifierType))
                    result.addError(new PolicyValidatorResult.Error(assertion, "Must be preceded by a credential source", null));

                if (holderOfKey && firstCertCred == -1 || firstCertCred > i) {
                    result.addError(new PolicyValidatorResult.Error(assertion, "Holder-of-Key selected, must be preceded by a certificate-based credential source", null));
                }

                if (decorateResponse && (firstRoute == -1 || firstRoute > i)) {
                    result.addError(new PolicyValidatorResult.Error(assertion, "Configured to decorate response, must be preceded by a routing assertion", null));
                }

                return;
            }
        }
    }
}
