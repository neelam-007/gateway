/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;

import java.lang.reflect.Constructor;

/**
 * Superclass for type mappings whose values can't be represented just as simple strings.
 */
class Pre32ComplexTypeMapping extends Pre32BasicTypeMapping {
    protected final Constructor constructor; // default, no-arguments constructor for this type

    Pre32ComplexTypeMapping(Class clazz, String externalName) {
        super(clazz, externalName);
        Constructor ctor;
        try {
            // Try to find the default constructor
            ctor = clazz.getConstructor(new Class[0]);
        } catch (Exception e) {
            ctor = null;
        }
        constructor = ctor;
    }

    Pre32ComplexTypeMapping(Class clazz, String externalName, Constructor constructor) {
        super(clazz, externalName);
        this.constructor = constructor;
    }

    Pre32ComplexTypeMapping(Class clazz, String externalName, String nsUri, String nsPrefix) {
        super(clazz, externalName, nsUri, nsPrefix);
        Constructor ctor;
        try {
            // Try to find the default constructor
            ctor = clazz.getConstructor(new Class[0]);
        } catch (Exception e) {
            ctor = null;
        }
        constructor = ctor;
    }

    protected Element freezeAnonymous(Pre32TypedReference object, Element container) {
        Element elm = container.getOwnerDocument().createElementNS(getNsUri(), getNsPrefix() + externalName);
        if (object.target == null)
            throw new Pre32InvalidPolicyTreeException("Null objects may not be serialized in Anonymous format");
        populateElement(elm, object);
        return elm;
    }

    protected Object stringToObject(String value) throws Pre32InvalidPolicyStreamException {
        if (!"included".equals(value))
            throw new Pre32InvalidPolicyStreamException("Complex type's value must be \"included\" if it is non-null");
        if (constructor == null)
            throw new Pre32InvalidPolicyStreamException("No default constructor known for class " + clazz);
        try {
            return constructor.newInstance(new Object[0]);
        } catch (Exception e) {
            throw new Pre32InvalidPolicyStreamException("Unable to construct class " + clazz, e);
        }
    }

    protected Pre32TypedReference thawAnonymous(Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        return createObject(source, "included", visitor);
    }

    protected String objectToString(Object value) throws Pre32InvalidPolicyTreeException {
        return value == null ? "null" : "included";
    }
}
