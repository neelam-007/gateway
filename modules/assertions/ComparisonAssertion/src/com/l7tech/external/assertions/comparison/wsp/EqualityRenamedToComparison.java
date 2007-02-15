/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.*;
import com.l7tech.external.assertions.comparison.ComparisonAssertion;
import org.w3c.dom.Element;

/**
 * @author alex
 */
public class EqualityRenamedToComparison {
    public static final TypeMapping equalityCompatibilityMapping =
        new CompatibilityAssertionMapping(new ComparisonAssertion(), "EqualityAssertion") {
            protected void configureAssertion(Assertion ass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                new BeanTypeMapping(ComparisonAssertion.class, "").populateObject(new TypedReference(ComparisonAssertion.class, ass), source, visitor);
            }
        };
}
