/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.logging.LogManager;
import org.jaxen.dom.DOMXPath;
import org.jaxen.JaxenException;

import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
public class RequestXpathAssertionValidator implements AssertionValidator {
    private static final Logger logger = LogManager.getInstance().getSystemLogger();

    public void validate( Assertion assertion, PolicyValidatorResult result ) {
        String pattern = ((RequestXpathAssertion)assertion).getPattern();
        if ( pattern == null ) {
            result.addError( new PolicyValidatorResult.Error( assertion, "XPath pattern is missing", null ) );
            logger.info("XPath pattern is missing");
            return;
        }

        try {
            new DOMXPath(pattern);
        } catch (JaxenException e) {
            result.addError( new PolicyValidatorResult.Error( assertion, "XPath pattern is not valid", e ) );
            logger.info("XPath pattern is not valid");
            return;
        }
        logger.fine("XPath patern is valid");
    }
}
