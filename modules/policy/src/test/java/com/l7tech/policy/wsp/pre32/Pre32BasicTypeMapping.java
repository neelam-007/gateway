/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;

import java.lang.reflect.Constructor;

/**
 * Pre32TypeMapping to use for basic concrete types whose values are represented most naturally by simple strings.
 */
class Pre32BasicTypeMapping implements Pre32TypeMapping {
    protected final String externalName;
    protected final Class clazz;
    protected final boolean isNullable;
    protected final Constructor stringConstructor;  // constructor-from-string, if this type has one
    protected final String nsPrefix;
    protected final String nsUri;

    Pre32BasicTypeMapping(Class clazz, String externalName, String nsUri, String prefix) {
        this.clazz = clazz;
        this.externalName = externalName;
        this.isNullable = Pre32TypeMappingUtils.isNullableType(clazz);
        this.nsUri = nsUri;
        this.nsPrefix = prefix;
        Constructor stringCons;
        try {
            stringCons = clazz.getConstructor(new Class[]{String.class});
        } catch (Exception e) {
            stringCons = null;
        }
        this.stringConstructor = stringCons;
    }

    Pre32BasicTypeMapping(Class clazz, String externalName) {
        this(clazz, externalName, Pre32WspConstants.L7_POLICY_NS, ""); // Default ot default NS with no NS prefix
    }

    public Class getMappedClass() { return clazz; }

    public String getExternalName() { return externalName; }

    public Element freeze(Pre32TypedReference object, Element container) {
        if (object == null)
            throw new IllegalArgumentException("a non-null Pre32TypedReference must be provided");
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
    protected Element freezeAnonymous(Pre32TypedReference object, Element container) {
        throw new IllegalArgumentException("Pre32BasicTypeMapping supports only Named format");
    }

    /**
     * Return the new element, without appending it to the container yet.
     */
    protected Element freezeNamed(Pre32TypedReference object, Element container) {
        Element elm = container.getOwnerDocument().createElementNS(getNsUri(), getNsPrefix() + object.name);
        if (object.target == null) {
            if (!isNullable)  // sanity check
                throw new Pre32InvalidPolicyTreeException("Assertion has property \"" + object.name + "\" which mustn't be null yet is");
            elm.setAttribute(externalName + "Null", "null");
        } else {
            String stringValue = objectToString(object.target);
            elm.setAttribute(externalName, stringValue);
            populateElement(elm, object); // hook for more complex types
        }
        return elm;
    }

    /**
     * @return the namespace prefix to use for serialized elements created by this type mapping, ie "l7p31:".
     *         Includes the trailing colon if any.  Never null, but may be empty if the elements should be
     *         created in the default namespace.
     */
    protected String getNsPrefix() {
        return nsPrefix;
    }

    /** @return the namespace URI to use for serialized elements created by this type mapping, ie Pre32WspConstants.L7_POLICY_NS_31. */
    protected String getNsUri() {
        return nsUri;
    }

    /**
     * Do any extra work that might be requried by this element.
     *
     * @param newElement the newly-created element that needs to have properties filled in from object, whose
     *                   target may not be null.
     * @param object
     */
    protected void populateElement(Element newElement, Pre32TypedReference object) throws Pre32InvalidPolicyTreeException {
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

    public Pre32TypedReference thaw(Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        try {
            return doThaw(source, visitor, false);
        } catch (Pre32InvalidPolicyStreamException e) {
            return doThaw(visitor.invalidElement(source, e), visitor, true);
        }
    }

    private Pre32TypedReference doThaw(Element source, Pre32WspVisitor visitor, boolean recursing)
            throws Pre32InvalidPolicyStreamException
    {
        if (Pre32TypeMappingUtils.findTypeName(source) == null)
            return thawAnonymous(source, visitor);
        return thawNamed(source, visitor);
    }

    protected Pre32TypedReference thawNamed(Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        return doThawNamed(source, visitor, false);
    }

    private Pre32TypedReference doThawNamed(Element source, Pre32WspVisitor visitor, boolean recursing) throws Pre32InvalidPolicyStreamException {
        String typeName = Pre32TypeMappingUtils.findTypeName(source);
        String value = source.getAttribute(typeName);        

        if (typeName.endsWith("Null") && typeName.length() > 4) {
            typeName = typeName.substring(0, typeName.length() - 4);
            value = null;
            if (!isNullable) {
                final Pre32InvalidPolicyStreamException e = new Pre32InvalidPolicyStreamException("Policy contains a null " + externalName);
                if (recursing) throw e;
                return doThawNamed(visitor.invalidElement(source, e), visitor, true);
            }
        }

        if (!externalName.equals(typeName)) {
            final Pre32InvalidPolicyStreamException e = new Pre32InvalidPolicyStreamException("Pre32TypeMapping for " + clazz + ": unrecognized attr " + typeName);
            if (recursing) throw e;
            return doThawNamed(visitor.invalidElement(source, e), visitor, true);
        }

        if (value == null)
            return new Pre32TypedReference(clazz, null, source.getLocalName());

        return createObject(source, value, visitor);
    }

    /**
     * Inspect the DOM element and construct the actual object, which at this point is known to be non-null.
     * The default implementation calls stringToObject(value) to create the object, and populateObject() to fill
     * out its fields.
     *
     * @param element The element being deserialized
     * @param value   The simple string value represented by element, if meaningful for this Pre32TypeMapping; otherwise "included"
     * @return A Pre32TypedReference to the newly deserialized object
     * @throws Pre32InvalidPolicyStreamException if the element cannot be deserialized
     */
    protected Pre32TypedReference createObject(Element element, String value, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        if (value == null)
            throw new Pre32InvalidPolicyStreamException("Null values not supported"); // can't happen
        Pre32TypedReference tr = new Pre32TypedReference(clazz, stringToObject(value), element.getLocalName());
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
    protected void populateObject(Pre32TypedReference object, Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        // no action required for simple types
    }

    protected Pre32TypedReference thawAnonymous(Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        throw new IllegalArgumentException("Pre32BasicTypeMapping supports only Named format");
    }

    /**
     * This method is responsible for constructing the newly deserialized object, but doesn't populate its fields.
     * For simple types, perform the reverse of objectToString.
     */
    protected Object stringToObject(String value) throws Pre32InvalidPolicyStreamException {
        if (stringConstructor == null)
            throw new Pre32InvalidPolicyStreamException("No stringToObject defined for Pre32TypeMapping for class " + clazz);
        try {
            return stringConstructor.newInstance(new Object[]{value});
        } catch (Exception e) {
            throw new Pre32InvalidPolicyStreamException("Unable to convert string into " + clazz, e);
        }
    };
}
