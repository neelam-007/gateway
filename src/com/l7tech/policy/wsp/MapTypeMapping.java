/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * TypeMapping that knows how to represent a {@link Map} with String keys in a policy XML document.
 */
class MapTypeMapping extends ComplexTypeMapping {
    // This is utterly grotesque, but it's all Java's fault.  Please close eyes here
    static final Constructor mapConstructor;

    static {
        try {
            mapConstructor = LinkedHashMap.class.getConstructor(new Class[0]);
        } catch (Exception e) {
            throw new LinkageError("Couldn't find LinkedHashMap's default constructor");
        }
    }
    // You may now open your eyes

    public MapTypeMapping() {
        super(Map.class, "mapValue", mapConstructor);
    }

    protected void populateElement(Element newElement, TypedReference object) {
        Map map = (Map)object.target;
        Set entries = map.entrySet();
        for (Iterator i = entries.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Object key = entry.getKey();
            if (key == null)
                throw new InvalidPolicyTreeException("Maps with null keys are not currently permitted within a policy");
            if (!(key instanceof String))
                throw new InvalidPolicyTreeException("Maps with non-string keys are not currently permitted within a policy");
            Object value = entry.getValue();
            //if (value != null && !(value instanceof String))
            //    throw new InvalidPolicyTreeException("Maps with non-string values are not currently permitted within a policy");
            Element entryElement = newElement.getOwnerDocument().createElementNS(WspConstants.L7_POLICY_NS, "entry");
            newElement.appendChild(entryElement);
            WspConstants.typeMappingString.freeze(new TypedReference(String.class, key, "key"), entryElement);
            WspConstants.typeMappingObject.freeze(new TypedReference(value.getClass(), value, "value"), entryElement);
            //typeMappingString.freeze(new TypedReference(String.class, value, "value"), entryElement);
        }
    }

    protected void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        Map map = (Map)object.target;
        List entryElements = TypeMappingUtils.getChildElements(source, "entry");
        for (Iterator i = entryElements.iterator(); i.hasNext();) {
            Element element = (Element)i.next();
            List keyValueElements = TypeMappingUtils.getChildElements(element);
            if (keyValueElements.size() != 2)
                throw new InvalidPolicyStreamException("Map entry does not have exactly two child elements (key and value)");
            Element keyElement = (Element)keyValueElements.get(0);
            if (keyElement == null || keyElement.getNodeType() != Node.ELEMENT_NODE || !"key".equals(keyElement.getLocalName()))
                throw new InvalidPolicyStreamException("Map entry first child element is not a key element");
            Element valueElement = (Element)keyValueElements.get(1);
            if (valueElement == null || valueElement.getNodeType() != Node.ELEMENT_NODE || !"value".equals(valueElement.getLocalName()))
                throw new InvalidPolicyStreamException("Map entry last child element is not a value element");

            TypedReference ktr = WspConstants.typeMappingObject.thaw(keyElement, visitor);
            if (!String.class.equals(ktr.type))
                throw new InvalidPolicyStreamException("Maps with non-string keys are not currently permitted within a policy");
            if (ktr.target == null)
                throw new InvalidPolicyStreamException("Maps with null keys are not currently permitted within a policy");
            String key = (String)ktr.target;

            TypedReference vtr = WspConstants.typeMappingObject.thaw(valueElement, visitor);
            map.put(key, vtr.target);
        }
    }
}
