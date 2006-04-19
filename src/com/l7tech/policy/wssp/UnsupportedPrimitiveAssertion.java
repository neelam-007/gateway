/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import org.apache.ws.policy.PrimitiveAssertion;

/**
 * @author mike
 */
class UnsupportedPrimitiveAssertion implements PrimitiveAssertionConverter {
    private final String message;

    public UnsupportedPrimitiveAssertion(String message) {
        this.message = message;
    }

    public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
        throw new PolicyConversionException(message + ": " + p.getName());
    }
}
