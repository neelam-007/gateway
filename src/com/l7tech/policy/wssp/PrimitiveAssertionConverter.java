/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import org.apache.ws.policy.PrimitiveAssertion;

/**
 * @author mike
 */
interface PrimitiveAssertionConverter {
    com.l7tech.policy.assertion.Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException;
}
