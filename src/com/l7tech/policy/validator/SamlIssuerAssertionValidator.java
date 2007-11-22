/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.validator;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * @author alex
 */
public class SamlIssuerAssertionValidator implements AssertionValidator {
    private final SamlIssuerAssertion assertion;
    private final boolean holderOfKey;
    private final boolean decorateResponse;
    private final Set<String> HOK_URIS = Collections.unmodifiableSet(new HashSet<String>() {{
        add(SamlConstants.CONFIRMATION_HOLDER_OF_KEY);
        add(SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY);
    }});

    public SamlIssuerAssertionValidator(SamlIssuerAssertion sia) {
        this.assertion = sia;
        // Only Holder of Key has validation rules
        final EnumSet<SamlIssuerAssertion.DecorationType> dts = assertion.getDecorationTypes();
        decorateResponse = dts != null && dts.contains(SamlIssuerAssertion.DecorationType.RESPONSE);
        holderOfKey = HOK_URIS.contains(assertion.getSubjectConfirmationMethodUri());
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
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
                } else if (ass instanceof RequestWssSaml) {
                    RequestWssSaml saml = (RequestWssSaml) ass;
                    final String[] scs = saml.getSubjectConfirmations();
                    // RequestWssSaml is only a cert-based credential source if it's Holder of Key and has the signature constraint
                    if (scs.length == 1 && HOK_URIS.contains(scs[0]) && saml.isRequireHolderOfKeyWithMessageSignature()) {
                        firstCertCred = i;
                    }
                }
            } else if (ass == assertion) {
                if (firstCreds == -1 || firstCreds > i) {
                    result.addError(new PolicyValidatorResult.Error(assertion, path, "Must be preceded by a credential source", null));
                }

                if (holderOfKey && firstCertCred == -1 || firstCertCred > i) {
                    result.addError(new PolicyValidatorResult.Error(assertion, path, "Holder-of-Key selected, must be preceded by a certificate-based credential source", null));
                }

                if (decorateResponse && (firstRoute == -1 || firstRoute > i)) {
                    result.addError(new PolicyValidatorResult.Error(assertion, path, "Configured to decorate response, must be preceded by a routing assertion", null));
                }

                return;
            }
        }
    }
}
