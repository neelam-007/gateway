/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Pre32TypeMapping that knows how to represent a {@link Map} with String keys in a policy XML document.
 */
class Pre32MapTypeMapping extends Pre32ComplexTypeMapping {
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

    public Pre32MapTypeMapping() {
        super(Map.class, "mapValue", mapConstructor);
    }

    protected void populateElement(Element newElement, Pre32TypedReference object) {
        Map map = (Map)object.target;
        Set entries = map.entrySet();
        for (Iterator i = entries.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            Object key = entry.getKey();
            if (key == null)
                throw new Pre32InvalidPolicyTreeException("Maps with null keys are not currently permitted within a policy");
            if (!(key instanceof String))
                throw new Pre32InvalidPolicyTreeException("Maps with non-string keys are not currently permitted within a policy");
            Object value = entry.getValue();
            //if (value != null && !(value instanceof String))
            //    throw new Pre32InvalidPolicyTreeException("Maps with non-string values are not currently permitted within a policy");
            Element entryElement = newElement.getOwnerDocument().createElementNS(getNsUri(), getNsPrefix() + "entry");
            newElement.appendChild(entryElement);
            Pre32WspConstants.typeMappingString.freeze(new Pre32TypedReference(String.class, key, "key"), entryElement);
            Pre32WspConstants.typeMappingObject.freeze(new Pre32TypedReference(value.getClass(), value, "value"), entryElement);
            //typeMappingString.freeze(new Pre32TypedReference(String.class, value, "value"), entryElement);
        }
    }

    protected void populateObject(Pre32TypedReference object, Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        Map map = (Map)object.target;
        List entryElements = Pre32TypeMappingUtils.getChildElements(source, "entry");
        for (Iterator i = entryElements.iterator(); i.hasNext();) {
            Element element = (Element)i.next();
            List keyValueElements = Pre32TypeMappingUtils.getChildElements(element);
            if (keyValueElements.size() != 2)
                throw new Pre32InvalidPolicyStreamException("Map entry does not have exactly two child elements (key and value)");
            Element keyElement = (Element)keyValueElements.get(0);
            if (keyElement == null || keyElement.getNodeType() != Node.ELEMENT_NODE || !"key".equals(keyElement.getLocalName()))
                throw new Pre32InvalidPolicyStreamException("Map entry first child element is not a key element");
            Element valueElement = (Element)keyValueElements.get(1);
            if (valueElement == null || valueElement.getNodeType() != Node.ELEMENT_NODE || !"value".equals(valueElement.getLocalName()))
                throw new Pre32InvalidPolicyStreamException("Map entry last child element is not a value element");

            Pre32TypedReference ktr = Pre32WspConstants.typeMappingObject.thaw(keyElement, visitor);
            if (!String.class.equals(ktr.type))
                throw new Pre32InvalidPolicyStreamException("Maps with non-string keys are not currently permitted within a policy");
            if (ktr.target == null)
                throw new Pre32InvalidPolicyStreamException("Maps with null keys are not currently permitted within a policy");
            String key = (String)ktr.target;

            Pre32TypedReference vtr = Pre32WspConstants.typeMappingObject.thaw(valueElement, visitor);
            map.put(key, vtr.target);
        }
    }
}
