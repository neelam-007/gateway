/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.lang.reflect.Constructor;

/**
 * TypeMapping to use for basic concrete types whose values are represented most naturally by simple strings.
 */
class BasicTypeMapping implements TypeMapping {
    protected String externalName;
    protected Class clazz;
    protected boolean isNullable;
    protected Constructor stringConstructor;  // constructor-from-string, if this type has one

    BasicTypeMapping(Class clazz, String externalName) {
        this.clazz = clazz;
        this.externalName = externalName;
        this.isNullable = TypeMappingUtils.isNullableType(clazz);
        try {
            stringConstructor = clazz.getConstructor(new Class[]{String.class});
        } catch (Exception e) {
            stringConstructor = null;
        }
    }

    public Class getMappedClass() { return clazz; }

    public String getExternalName() { return externalName; }

    public Element freeze(TypedReference object, Element container) {
        if (object == null)
            throw new IllegalArgumentException("a non-null TypedReference must be provided");
        if (container == null)
            throw new IllegalArgumentException("a non-null container must be provided");
        if (object.type != clazz)
            throw new IllegalArgumentException("this TypeMapper is only for " + clazz + "; can't use with " + object.type);
        Element elm = object.name == null ? freezeAnonymous(object, container) : freezeNamed(object, container);
        container.appendChild(elm);
        return elm;
    }

    /**
     * Return the new element, without appending it to the container yet.
     */
    protected Element freezeAnonymous(TypedReference object, Element container) {
        throw new IllegalArgumentException("BasicTypeMapping supports only Named format");
    }

    /**
     * Return the new element, without appending it to the container yet.
     */
    protected Element freezeNamed(TypedReference object, Element container) {
        Element elm = container.getOwnerDocument().createElementNS(WspConstants.L7_POLICY_NS, object.name);
        if (object.target == null) {
            if (!isNullable)  // sanity check
                throw new InvalidPolicyTreeException("Assertion has property \"" + object.name + "\" which mustn't be null yet is");
            elm.setAttribute(externalName + "Null", "null");
        } else {
            String stringValue = objectToString(object.target);
            elm.setAttribute(externalName, stringValue);
            populateElement(elm, object); // hook for more complex types
        }
        return elm;
    }

    /**
     * Do any extra work that might be requried by this element.
     *
     * @param newElement the newly-created element that needs to have properties filled in from object, whose
     *                   target may not be null.
     * @param object
     */
    protected void populateElement(Element newElement, TypedReference object) throws InvalidPolicyTreeException {
        // no action required for simple types
    }

    /**
     * Convert object into a string that can be saved as an attribute value.
     *
     * @param target the object to examine. must not be null
     * @return the object in string form, or null if the object was null
     */
    protected String objectToString(Object target) {
        return target.toString();
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        try {
            return doThaw(source, visitor, false);
        } catch (InvalidPolicyStreamException e) {
            return doThaw(visitor.invalidElement(source, e), visitor, true);
        }
    }

    private TypedReference doThaw(Element source, WspVisitor visitor, boolean recursing)
            throws InvalidPolicyStreamException
    {
        NamedNodeMap attrs = source.getAttributes();
        switch (attrs.getLength()) {
            case 0:
                // Anonymous element
                return thawAnonymous(source, visitor);

            case 1:
                // Named element
                return thawNamed(source, visitor);

            default:
                final BadAttributeCountException e = new BadAttributeCountException(
                        "Policy contains a " + source.getNodeName() +
                        " element with more than one attribute");
                if (recursing) throw e;
                return doThaw(visitor.invalidElement(source, e), visitor, true);
        }
    }

    protected TypedReference thawNamed(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        return doThawNamed(source, visitor, false);
    }

    private TypedReference doThawNamed(Element source, WspVisitor visitor, boolean recursing) throws InvalidPolicyStreamException {
        NamedNodeMap attrs = source.getAttributes();
        if (attrs.getLength() != 1)
        {
            final BadAttributeCountException e = new BadAttributeCountException("Policy contains a " +
                                                                                                   source.getNodeName() +
                                                                                                   " element that doesn't have exactly one attribute");
            if (recursing) throw e;
            return doThawNamed(visitor.invalidElement(source, e), visitor, true);
        }
        Node attr = attrs.item(0);
        String typeName = attr.getLocalName();
        if (typeName == null) typeName = attr.getNodeName();
        String value = attr.getNodeValue();

        if (typeName.endsWith("Null") && typeName.length() > 4) {
            typeName = typeName.substring(0, typeName.length() - 4);
            value = null;
            if (!isNullable) {
                final InvalidPolicyStreamException e = new InvalidPolicyStreamException("Policy contains a null " + externalName);
                if (recursing) throw e;
                return doThawNamed(visitor.invalidElement(source, e), visitor, true);
            }
        }

        if (!externalName.equals(typeName)) {
            final InvalidPolicyStreamException e = new InvalidPolicyStreamException("TypeMapping for " + clazz + ": unrecognized attr " + typeName);
            if (recursing) throw e;
            return doThawNamed(visitor.invalidElement(source, e), visitor, true);
        }

        if (value == null)
            return new TypedReference(clazz, null, source.getNodeName());

        return createObject(source, value, visitor);
    }

    /**
     * Inspect the DOM element and construct the actual object, which at this point is known to be non-null.
     * The default implementation calls stringToObject(value) to create the object, and populateObject() to fill
     * out its fields.
     *
     * @param element The element being deserialized
     * @param value   The simple string value represented by element, if meaningful for this TypeMapping; otherwise "included"
     * @return A TypedReference to the newly deserialized object
     * @throws InvalidPolicyStreamException if the element cannot be deserialized
     */
    protected TypedReference createObject(Element element, String value, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (value == null)
            throw new InvalidPolicyStreamException("Null values not supported"); // can't happen
        TypedReference tr = new TypedReference(clazz, stringToObject(value), element.getNodeName());
        if (tr.target != null)
            populateObject(tr, element, visitor);
        return tr;
    }

    /**
     * Do any extra work that might be requried by this new object deserialized from this element.
     *
     * @param object the newly-created object that needs to have properties filled in from source. target may not be null
     * @param source the element from which object is being created
     */
    protected void populateObject(TypedReference object, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        // no action required for simple types
    }

    protected TypedReference thawAnonymous(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        throw new IllegalArgumentException("BasicTypeMapping supports only Named format");
    }

    /**
     * This method is responsible for constructing the newly deserialized object, but doesn't populate its fields.
     * For simple types, perform the reverse of objectToString.
     */
    protected Object stringToObject(String value) throws InvalidPolicyStreamException {
        if (stringConstructor == null)
            throw new InvalidPolicyStreamException("No stringToObject defined for TypeMapping for class " + clazz);
        try {
            return stringConstructor.newInstance(new Object[]{value});
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to convert string into " + clazz, e);
        }
    };
}
