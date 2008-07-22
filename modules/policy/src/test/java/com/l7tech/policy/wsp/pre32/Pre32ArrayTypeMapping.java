/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Pre32TypeMapping that knows how to serialize arrays of objects of some specific class.
 */
class Pre32ArrayTypeMapping extends Pre32ComplexTypeMapping {
    private final Object[] prototype;

    public Pre32ArrayTypeMapping(Object[] prototype, String externalName) {
        super(prototype.getClass(), externalName);
        this.prototype = prototype;
    }

    public Pre32ArrayTypeMapping(Object[] prototype, String externalName, String nsUri, String nsPrefix) {
        super(prototype.getClass(), externalName, nsUri, nsPrefix);
        this.prototype = prototype;
    }

    protected void populateElement(Element newElement, Pre32TypedReference object) throws Pre32InvalidPolicyTreeException {
        Object[] array = (Object[])object.target;
        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            Pre32WspConstants.typeMappingObject.freeze(new Pre32TypedReference(Object.class, o, "item"), newElement);
        }
    }

    protected Pre32TypedReference createObject(Element element, String value, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        List objects = new ArrayList();
        List arrayElements = Pre32TypeMappingUtils.getChildElements(element, "item");
        for (Iterator i = arrayElements.iterator(); i.hasNext();) {
            Element kidElement = (Element)i.next();
            Pre32TypedReference ktr = Pre32WspConstants.typeMappingObject.thaw(kidElement, visitor);
            objects.add(ktr.target);
        }
        try {
            return new Pre32TypedReference(clazz, objects.toArray(prototype), element.getLocalName());
        } catch (ArrayStoreException e) {
            throw new Pre32InvalidPolicyStreamException("Array item with incompatible type", e);
        }
    }
}
