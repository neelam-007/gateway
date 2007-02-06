/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * TypeMapping that knows how to serliaze a CompositeAssertion and its children into a policy XML document.
 */
class CompositeAssertionMapping implements TypeMapping {
    private Class clazz;
    private String externalName;
    private CompositeAssertion source;

    CompositeAssertionMapping(CompositeAssertion a, String externalName) {
        this.clazz = a.getClass();
        this.externalName = externalName;
        makeAssertion(externalName); // consistency check
    }

    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
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
                tm = (TypeMapping)kid.meta().get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE);
            if (tm == null)
                throw new InvalidPolicyTreeException("No TypeMapping found for class " + kidClass);
            tm.freeze(wspWriter, new TypedReference(kidClass, kid), element);
        }
    }

    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
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

    public Class getMappedClass() { return clazz; }

    public String getExternalName() { return externalName; }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        Element element;
        if (wspWriter.isPre32Compat()) {
            element = XmlUtil.createAndAppendElementNS(container, externalName, WspConstants.L7_POLICY_NS, "wsp");
        } else {
            element = XmlUtil.createAndAppendElementNS(container, externalName, WspConstants.WSP_POLICY_NS, "wsp");
            element.setAttributeNS(WspConstants.WSP_POLICY_NS, "wsp:Usage", "Required");
        }
        populateElement(wspWriter, element, object);
        return element;
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        CompositeAssertion cass = makeAssertion(externalName);
        populateObject(cass, source, visitor);
        return new TypedReference(clazz, cass);
    }

    private CompositeAssertion makeAssertion(String externalName) throws IllegalArgumentException {
        CompositeAssertion cass;
        if ("OneOrMore".equals(externalName)) {
            cass = new OneOrMoreAssertion();
        } else if ("All".equals(externalName)) {
            cass = new AllAssertion();
        } else if (externalName.equals("ExactlyOne")) {
            cass = new ExactlyOneAssertion();
        } else {
            throw new IllegalArgumentException("Unknown externalName");
        }
        return cass;
    }
}
