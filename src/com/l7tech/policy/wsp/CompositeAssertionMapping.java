/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * TypeMapping that knows how to serliaze a CompositeAssertion and its children into a policy XML document.
 */
class CompositeAssertionMapping extends AssertionMapping {
    CompositeAssertion source;

    CompositeAssertionMapping(CompositeAssertion a, String externalName) {
        super(a, externalName);
    }

    protected void populateElement(Element element, TypedReference object) throws InvalidPolicyTreeException {
        // Do not serialize any properties of the CompositeAssertion itself: shouldn't be any, and it'll include kid list
        // NO super.populateElement(element, object);
        CompositeAssertion cass = (CompositeAssertion)object.target;

        List kids = cass.getChildren();
        for (Iterator i = kids.iterator(); i.hasNext();) {
            Assertion kid = (Assertion)i.next();
            if (kid == null)
                throw new InvalidPolicyTreeException("Unable to serialize a null assertion");
            Class kidClass = kid.getClass();
            TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(kidClass);
            if (tm == null)
                throw new InvalidPolicyTreeException("No TypeMapping found for class " + kidClass);
            tm.freeze(new TypedReference(kidClass, kid), element);
        }
    }

    protected void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        // Do not deserialize any properties of the CompositeAssertion itself: shouldn't be any, and it'll include kid list
        // NO super.populateObject(object, source);
        CompositeAssertion cass = (CompositeAssertion)object.target;

        // gather children
        List convertedKids = new LinkedList();
        List kids = TypeMappingUtils.getChildElements(source);
        for (Iterator i = kids.iterator(); i.hasNext();) {
            Element kidNode = (Element)i.next();
            TypedReference tr = WspConstants.typeMappingObject.thaw(kidNode, visitor);
            if (tr.target == null)
                throw new InvalidPolicyStreamException("CompositeAssertion " + cass + " has null child");
            convertedKids.add(tr.target);
        }
        cass.setChildren(convertedKids);
    }
}
