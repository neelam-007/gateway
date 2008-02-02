/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ncesval;

import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.common.xml.Wsdl;

/** @author alex */
public class NcesValidatorAssertionValidator implements AssertionValidator {
    private final NcesValidatorAssertion assertion;
    private final boolean samlRequired;

    public NcesValidatorAssertionValidator(NcesValidatorAssertion assertion) {
        this.assertion = assertion;
        this.samlRequired = assertion.isSamlRequired();
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (!samlRequired) return;

        int firstSaml = -1;
        for (int i = 0; i < path.getPath().length; i++) {
            Assertion ass = path.getPath()[i];
            if (ass instanceof RequestWssSaml) {
                firstSaml = i;
            } else if (ass instanceof NcesValidatorAssertion) {
                if (ass != assertion) continue;

                if (firstSaml == -1 || firstSaml > i) {
                    result.addWarning(
                        new PolicyValidatorResult.Warning(assertion, path,
                          "Detailed SAML Assertion validation must be done by a separate SAML Validation Assertion", null));
                }
                return;
            }
        }
    }
}
