/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TypeMapping that knows how to serialize arrays of objects of some specific class.
 */
class ArrayTypeMapping extends ComplexTypeMapping {
    private final Object[] prototype;

    public ArrayTypeMapping(Object[] prototype, String externalName) {
        super(prototype.getClass(), externalName);
        this.prototype = prototype;
    }

    public ArrayTypeMapping(Object[] prototype, String externalName, String nsUri, String nsPrefix) {
        super(prototype.getClass(), externalName, nsUri, nsPrefix);
        this.prototype = prototype;
    }

    protected void populateElement(Element newElement, TypedReference object) throws InvalidPolicyTreeException {
        Object[] array = (Object[])object.target;
        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            WspConstants.typeMappingObject.freeze(new TypedReference(Object.class, o, "item"), newElement);
        }
    }

    protected TypedReference createObject(Element element, String value, WspVisitor visitor) throws InvalidPolicyStreamException {
        List objects = new ArrayList();
        List arrayElements = TypeMappingUtils.getChildElements(element, "item");
        for (Iterator i = arrayElements.iterator(); i.hasNext();) {
            Element kidElement = (Element)i.next();
            TypedReference ktr = WspConstants.typeMappingObject.thaw(kidElement, visitor);
            objects.add(ktr.target);
        }
        try {
            return new TypedReference(clazz, objects.toArray(prototype), element.getLocalName());
        } catch (ArrayStoreException e) {
            throw new InvalidPolicyStreamException("Array item with incompatible type", e);
        }
    }
}
