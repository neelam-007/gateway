/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.validator;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.service.PublishedService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Validates the SAML statement constraints in the context of the policy
 * and the service.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class SamlStatementValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(SamlStatementValidator.class.getName());
    private final RequestWssSaml requestWssSaml;

    public SamlStatementValidator(RequestWssSaml sa) {
        requestWssSaml = sa;
    }

    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
        List confirmations = Arrays.asList(requestWssSaml.getSubjectConfirmations());
        SslAssertion[]  sslAssertions = getSslAssertions(path);

        if (requestWssSaml.isNoSubjectConfirmation()) {
            if (sslAssertions.length == 0) {
                String message = "SSL is recommended with SAML assertions with No Subject Confirmation";
                result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
            }
        }

        if (confirmations.contains(SamlConstants.CONFIRMATION_BEARER)) {
            if (sslAssertions.length == 0) {
                String message = "SSL is recommended with SAML assertions with Bearer Subject Confirmation";
                result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
            }
        }

        if (confirmations.contains(SamlConstants.CONFIRMATION_HOLDER_OF_KEY) ||
            confirmations.contains(SamlConstants.CONFIRMATION_SENDER_VOUCHES)) {
            if (!requestWssSaml.isRequireProofOfPosession()) {
                boolean hasSslAsCrendentialSource = false;
                for (int i = 0; i < sslAssertions.length; i++) {
                    SslAssertion sslAssertion = sslAssertions[i];
                    if (sslAssertion.isCredentialSource()) {
                        hasSslAsCrendentialSource = true;
                        break;
                    }
                }
                if (!hasSslAsCrendentialSource) {
                    String message = "SSL with Client Certificates must be used when No Proof Of Posession specified.";
                    result.addError((new PolicyValidatorResult.Error(requestWssSaml, path, message, null)));
                }
            }
        }


    }

    private SslAssertion[] getSslAssertions(AssertionPath path) {
       Collection sslAssertions = new ArrayList();
        Assertion[] pathArray = path.getPath();
        for (int i = 0; i < pathArray.length; i++) {
            Assertion assertion = pathArray[i];
            if (assertion instanceof SslAssertion) {
                sslAssertions.add(assertion);
            }
        }
        return (SslAssertion[])sslAssertions.toArray(new SslAssertion[] {});
    }

    private static final String INCOMPATIBLE_WITH_PROOF_OF_POSSESION = "The subject confirmation {0} is not compatible with the proof of posession";
}
