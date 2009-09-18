/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ncesval;

import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.wsdl.Wsdl;

/** @author alex */
public class NcesValidatorAssertionValidator implements AssertionValidator {
    private final NcesValidatorAssertion assertion;
    private final boolean samlRequired;

    public NcesValidatorAssertionValidator(NcesValidatorAssertion assertion) {
        this.assertion = assertion;
        this.samlRequired = assertion.isSamlRequired();
    }

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        int firstSaml = -1;
        for (int i = 0; i < path.getPath().length; i++) {
            Assertion ass = path.getPath()[i];
            if (!ass.isEnabled()) continue;
            if (ass instanceof RequireWssSaml) {
                firstSaml = i;
            } else if (ass instanceof NcesValidatorAssertion) {
                if (ass != assertion) continue;

                if ( samlRequired ) {
                    if (firstSaml == -1 || firstSaml > i) {
                        result.addWarning(
                            new PolicyValidatorResult.Warning(assertion, path,
                              "Detailed SAML Assertion validation must be done by a separate SAML Validation Assertion", null));
                    }
                }

                if ( (assertion.getTrustedCertificateInfo() == null || assertion.getTrustedCertificateInfo().length == 0) &&
                     (assertion.getTrustedIssuerCertificateInfo() == null || assertion.getTrustedIssuerCertificateInfo().length == 0) ) {
                    result.addWarning(
                        new PolicyValidatorResult.Warning(assertion, path,
                          "No trusted certificates or trusted certificate issuers selected, assertion will always fail", null));                    
                }

                return;
            }
        }
    }
}
