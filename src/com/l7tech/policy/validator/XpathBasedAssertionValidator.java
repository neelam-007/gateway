/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import org.jaxen.dom.DOMXPath;

import java.util.logging.Logger;

/**
 * @author mike
 * @version 1.0
 */
public class XpathBasedAssertionValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(XpathBasedAssertionValidator.class.getName());
    private final XpathBasedAssertion assertion;

    public XpathBasedAssertionValidator(XpathBasedAssertion ra) {
        assertion = ra;
    }

    public void validate(AssertionPath path, PolicyValidatorResult result) {
        String pattern = assertion.getXpathExpression().getExpression();
        if (pattern == null) {
            result.addError(new PolicyValidatorResult.Error(assertion, path, "XPath pattern is missing", null));
            logger.info("XPath pattern is missing");
            return;
        }

        try {
            new DOMXPath(pattern);
        } catch (Exception e) {
            result.addError(new PolicyValidatorResult.Error(assertion, path, "XPath pattern is not valid", e));
            logger.info("XPath pattern is not valid");
            return;
        }
    }
}
