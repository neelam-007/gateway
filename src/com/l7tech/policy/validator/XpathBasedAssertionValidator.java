/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.service.PublishedService;
import org.jaxen.dom.DOMXPath;

import java.util.logging.Logger;

/**
 * @author mike
 * @version 1.0
 */
public class XpathBasedAssertionValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(XpathBasedAssertionValidator.class.getName());
    private final XpathBasedAssertion assertion;
    private String errString;
    private Throwable errThrowable;

    public XpathBasedAssertionValidator(XpathBasedAssertion ra) {
        assertion = ra;
        String pattern = null;
        if (assertion.getXpathExpression() != null)
            pattern = assertion.getXpathExpression().getExpression();

        if (pattern == null) {
            errString = "XPath pattern is missing";
            logger.info(errString);
        } else {
            try {
                new DOMXPath(pattern);
            } catch (Exception e) {
                errString = "XPath pattern is not valid";
                errThrowable = e;
            }
        }
    }

    public void validate(AssertionPath path, PublishedService service, PolicyValidatorResult result) {
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, path, errString, errThrowable));
    }
}
