/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Pre32TypeMapping that knows how to serliaze a CompositeAssertion and its children into a policy XML document.
 */
class Pre32CompositeAssertionMapping extends Pre32AssertionMapping {
    CompositeAssertion source;

    Pre32CompositeAssertionMapping(CompositeAssertion a, String externalName) {
        super(a, externalName);
    }

    protected void populateElement(Element element, Pre32TypedReference object) throws Pre32InvalidPolicyTreeException {
        // Do not serialize any properties of the CompositeAssertion itself: shouldn't be any, and it'll include kid list
        // NO super.populateElement(element, object);
        CompositeAssertion cass = (CompositeAssertion)object.target;

        List kids = cass.getChildren();
        for (Iterator i = kids.iterator(); i.hasNext();) {
            Assertion kid = (Assertion)i.next();
            if (kid == null)
                throw new Pre32InvalidPolicyTreeException("Unable to serialize a null assertion");
            Class kidClass = kid.getClass();
            Pre32TypeMapping tm = Pre32TypeMappingUtils.findTypeMappingByClass(kidClass);
            if (tm == null)
                throw new Pre32InvalidPolicyTreeException("No Pre32TypeMapping found for class " + kidClass);
            tm.freeze(new Pre32TypedReference(kidClass, kid), element);
        }
    }

    protected void populateObject(Pre32TypedReference object, Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        // Do not deserialize any properties of the CompositeAssertion itself: shouldn't be any, and it'll include kid list
        // NO super.populateObject(object, source);
        CompositeAssertion cass = (CompositeAssertion)object.target;

        // gather children
        List convertedKids = new LinkedList();
        List kids = Pre32TypeMappingUtils.getChildElements(source);
        for (Iterator i = kids.iterator(); i.hasNext();) {
            Element kidNode = (Element)i.next();
            Pre32TypedReference tr = Pre32WspConstants.typeMappingObject.thaw(kidNode, visitor);
            if (tr.target == null)
                throw new Pre32InvalidPolicyStreamException("CompositeAssertion " + cass + " has null child");
            convertedKids.add(tr.target);
        }
        cass.setChildren(convertedKids);
    }
}
