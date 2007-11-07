/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.validator;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SamlIssuerAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.service.PublishedService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author alex
 */
public class SamlIssuerAssertionValidator implements AssertionValidator {
    private final SamlIssuerAssertion assertion;
    private final boolean validate;
    private final Set<String> HOK_URIS = Collections.unmodifiableSet(new HashSet<String>() {{
        add(SamlConstants.CONFIRMATION_HOLDER_OF_KEY);
        add(SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY);
    }});

    public SamlIssuerAssertionValidator(SamlIssuerAssertion sia) {
        this.assertion = sia;
        // Only Holder of Key has validation rules
        validate = HOK_URIS.contains(assertion.getSubjectConfirmationMethodUri());
    }

    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
        if (!validate) return;

        int firstCertCred = -1;
        for (int i = 0; i < path.getPath().length; i++) {
            Assertion ass = path.getPath()[i];
            if (ass.isCredentialSource()) {
                if (ass instanceof RequestWssX509Cert || ass instanceof SslAssertion || ass instanceof SecureConversation) {
                    firstCertCred = i;
                } else if (ass instanceof RequestWssSaml) {
                    RequestWssSaml saml = (RequestWssSaml) ass;
                    final String confUri = saml.getSubjectConfirmations()[0];
                    // RequestWssSaml is only a cert-based credential source if it's Holder of Key and has the signature constraint
                    if (saml.getSubjectConfirmations().length == 1 && HOK_URIS.contains(confUri) && saml.isRequireHolderOfKeyWithMessageSignature()) {
                        firstCertCred = i;
                    }
                }
            } else if (ass == assertion && firstCertCred == -1 || firstCertCred > i) {
                result.addError(new PolicyValidatorResult.Error(assertion, path, "SAML Issuer Assertion with Holder-of-Key must be preceded by a certificate-based credential source", null));
                return;
            }
        }
    }
}
