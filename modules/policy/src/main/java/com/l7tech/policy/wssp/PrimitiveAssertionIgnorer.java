/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import org.apache.ws.policy.PrimitiveAssertion;

import java.util.logging.Logger;

/**
 * @author mike
 */
class PrimitiveAssertionIgnorer implements PrimitiveAssertionConverter {
    private static final Logger logger = Logger.getLogger(PrimitiveAssertionIgnorer.class.getName());
    public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
        // Ignore this property
        logger.finest("Ignoring property: " + p.getName());
        return null;
    }
}
