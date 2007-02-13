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

import java.util.LinkedList;
import java.util.List;

/**
 * TypeMapping that knows how to serliaze a CompositeAssertion and its children into a policy XML document.
 */
class CompositeAssertionMapping implements TypeMapping {
    private final CompositeAssertion prototype;
    private Class<? extends CompositeAssertion> clazz;
    private String externalName;

    CompositeAssertionMapping(CompositeAssertion a, String externalName) {
        this.clazz = a.getClass();
        this.externalName = externalName;
        prototype = makeAssertion(externalName); // consistency check
    }

    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        // Do not serialize any properties of the CompositeAssertion itself: shouldn't be any, and it'll include kid list
        // NO super.populateElement(element, object);
        CompositeAssertion cass = (CompositeAssertion)object.target;

        //noinspection unchecked
        List<Assertion> kids = cass.getChildren();
        for (Assertion kid : kids) {
            if (kid == null)
                throw new InvalidPolicyTreeException("Unable to serialize a null assertion");
            Class<? extends Object> kidClass = kid.getClass();
            TypeMapping tm = TypeMappingUtils.findTypeMappingByClass(kidClass, wspWriter);
            if (tm == null)
                tm = (TypeMapping)kid.meta().get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE);
            if (tm == null)
                throw new InvalidPolicyTreeException("No TypeMapping found for class " + kidClass);
            tm.freeze(wspWriter, new TypedReference(kidClass, kid), element);
        }
    }

    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        // gather children
        List<Object> convertedKids = new LinkedList<Object>();
        List<Element> kids = TypeMappingUtils.getChildElements(source);
        for (Element kid : kids) {
            TypedReference tr = WspConstants.typeMappingObject.thaw(kid, visitor);
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

    public TypeMappingFinder getSubtypeFinder() {
        return (TypeMappingFinder)prototype.meta().get(AssertionMetadata.WSP_SUBTYPE_FINDER);
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
