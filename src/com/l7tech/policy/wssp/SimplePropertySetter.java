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
class SimplePropertySetter implements PrimitiveAssertionConverter {
    private final String propertyName;
    private final boolean propertyValue;

    public SimplePropertySetter(String propertyName, boolean propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public Assertion convert(WsspVisitor v, PrimitiveAssertion p) throws PolicyConversionException {
        v.setSimpleProperty(propertyName, propertyValue);
        return null;
    }
}
