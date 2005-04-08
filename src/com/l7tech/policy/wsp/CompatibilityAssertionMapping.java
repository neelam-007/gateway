/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import org.w3c.dom.Element;

/**
 * A mapping that knows how to read, but not write, obsolete and now-nonexistent Layer 7 policy assertions.
 */
abstract class CompatibilityAssertionMapping extends AssertionMapping {
    /**
     * Create a new CompatibilityAssertionMapping that will thaw elements with the specified externalName
     * as a new, empty instance of the specified Assertion, and then call {@link #populateObject} to fill in
     * the details.
     *
     * @param a             a prototype instance of the modern assertion that should be used in place of the old assertion.
     * @param externalName  the old assertion's external name, as present in old policy documents.
     */
    public CompatibilityAssertionMapping(Assertion a, String externalName) {
        super(a, externalName);
    }

    public final Element freeze(TypedReference object, Element container) {
        // Shouldn't be possible for this to ever happen
        throw new InvalidPolicyTreeException("Unable to create new policies containing " + externalName + "; can only read them for backward-compatibility purposes");
    }

    protected void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        configureAssertion((Assertion)object.target, source, visitor);
    };

    /**
     * Configure the newly-created assertion that is going to take the place of the no-longer-supported assertion.
     *
     * @param assertion   a newly-created assertion of the type passed to the constructor.  Never null.
     * @param source      the XML representing the no-longer-supported assertion.  Never null.
     * @param visitor     the visitor to use if any subelements need to be parsed, or if invalid elements or properties need to be reported.
     * @throws InvalidPolicyStreamException  if the new assertion cannot be configured.
     */
    protected abstract void configureAssertion(Assertion assertion, Element source, WspVisitor visitor) throws InvalidPolicyStreamException;
}
