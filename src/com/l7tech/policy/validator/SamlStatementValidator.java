/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.service.PublishedService;

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
        if (requestWssSaml.isRequireProofOfPosession()) {
            return;
        }
        if (!path.contains(SslAssertion.class)) {
            String message = "SSL is recommended when no proof of posession specified.";
            result.addWarning((new PolicyValidatorResult.Warning(requestWssSaml, path, message, null)));
            logger.info(message);
        }
    }
}
