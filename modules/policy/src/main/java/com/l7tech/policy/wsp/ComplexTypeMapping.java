/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

import java.lang.reflect.Constructor;

/**
 * Superclass for type mappings whose values can't be represented just as simple strings.
 */
public class ComplexTypeMapping extends BasicTypeMapping {
    protected final Constructor constructor; // default, no-arguments constructor for this type

    public ComplexTypeMapping(Class clazz, String externalName) {
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

    public ComplexTypeMapping(Class clazz, String externalName, Constructor constructor) {
        super(clazz, externalName);
        this.constructor = constructor;
    }

    protected Element freezeAnonymous(WspWriter wspWriter, TypedReference object, Element container) {
        Element elm = container.getOwnerDocument().createElementNS(getNsUri(), getNsPrefix() + externalName);
        if (object.target == null)
            throw new InvalidPolicyTreeException("Null objects may not be serialized in Anonymous format");
        populateElement(wspWriter, elm, object);
        return elm;
    }

    protected Object stringToObject(String value) throws InvalidPolicyStreamException {
        if (!"included".equals(value))
            throw new InvalidPolicyStreamException("Complex type's value must be \"included\" if it is non-null");
        if (constructor == null)
            throw new InvalidPolicyStreamException("No default constructor known for class " + clazz);
        try {
            return constructor.newInstance(new Object[0]);
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to construct class " + clazz, e);
        }
    }

    protected TypedReference thawAnonymous(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        return createObject(source, "included", visitor);
    }

    protected String objectToString(Object value) throws InvalidPolicyTreeException {
        return value == null ? "null" : "included";
    }
}
