/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author mike
 * @version 1.0
 */
public class XpathBasedAssertionValidator extends NamespaceMigratableAssertionValidator {
    private static final Logger logger = Logger.getLogger(XpathBasedAssertionValidator.class.getName());

    private final XpathBasedAssertion assertion;
    private String errString;
    private Throwable errThrowable;

    public XpathBasedAssertionValidator( final XpathBasedAssertion xpathBasedAssertion ) {
        super(xpathBasedAssertion);
        assertion = xpathBasedAssertion;
        String pattern = null;
        final XpathExpression xpathExpression = assertion.getXpathExpression();
        if (xpathExpression != null)
            pattern = xpathExpression.getExpression();

        if (pattern == null) {
            errString = "XPath pattern is missing";
            logger.info(errString);
        } else {
            try {
                final Map<String,String> namespaces = xpathBasedAssertion.namespaceMap();
                XpathUtil.testXpathExpression(null, pattern, xpathExpression.getXpathVersion(), namespaces, null);
            } catch (Exception e) {
                errString = "XPath pattern is not valid";
                errThrowable = e;
            }
        }
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate(path, pvc, result);
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, errString, errThrowable));
    }
}
