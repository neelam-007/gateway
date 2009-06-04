/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.DomUtils;

import org.w3c.dom.Element;

import java.lang.reflect.Constructor;

/**
 * TypeMapping to use for basic concrete types whose values are represented most naturally by simple strings.
 */
public class BasicTypeMapping implements TypeMapping {
    protected final String externalName;
    protected final Class clazz;
    protected final String sinceVersion;
    protected final boolean isNullable;
    protected final Constructor stringConstructor;  // constructor-from-string, if this type has one
    protected final String nsPrefix;
    protected final String nsUri;

    public BasicTypeMapping(Class clazz, String externalName) {
        this( clazz, externalName, null );
    }

    public BasicTypeMapping(Class clazz, String externalName, String version) {
        this.clazz = clazz;
        this.externalName = externalName;
        this.sinceVersion = version;
        this.isNullable = TypeMappingUtils.isNullableType(clazz);
        this.nsUri = WspConstants.L7_POLICY_NS;
        this.nsPrefix = "L7p:";
        Constructor stringCons;
        try {
            stringCons = clazz.getConstructor(new Class[]{String.class});
        } catch (Exception e) {
            stringCons = null;
        }
        this.stringConstructor = stringCons;
    }

    public Class getMappedClass() { return clazz; }

    public String getSinceVersion() { return sinceVersion; }

    public String getExternalName() { return externalName; }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (object == null)
            throw new IllegalArgumentException("a non-null TypedReference must be provided");
        if (container == null)
            throw new IllegalArgumentException("a non-null container must be provided");

        Class typeClass =TypeMappingUtils.getClassForType(object.type);
        if(!typeClass.equals(clazz) )
            throw new IllegalArgumentException("this TypeMapper is only for " + clazz + "; can't use with " + typeClass);

        Element elm = object.name == null ? freezeAnonymous(wspWriter, object, container) : freezeNamed(wspWriter, object, container);
        container.appendChild(elm);
        return elm;
    }

    static class NotNamedFormatException extends IllegalArgumentException {
        public NotNamedFormatException(String s) {
            super(s);
        }
    }

    /**
     * Return the new element, without appending it to the container yet.
     */
    protected Element freezeAnonymous(WspWriter wspWriter, TypedReference object, Element container) {
        throw new NotNamedFormatException("BasicTypeMapping supports only Named format");
    }

    /**
     * Return the new element, without appending it to the container yet.
     */
    protected Element freezeNamed(WspWriter wspWriter, TypedReference object, Element container) {
        Element elm = container.getOwnerDocument().createElementNS(getNsUri(), getNsPrefix() + object.name);
        if (object.target == null) {
            if (!isNullable)  // sanity check
                throw new InvalidPolicyTreeException("Assertion has property \"" + object.name + "\" which mustn't be null yet is");
            elm.setAttribute(externalName + "Null", "null");
        } else {
            String stringValue = objectToString(object.target);

            if ( serializeAsAttribute( stringValue ) ) {
                elm.setAttribute(externalName, stringValue);
            } else {
                elm.setAttribute(externalName + "Reference", "inline");
                elm.appendChild(container.getOwnerDocument().createCDATASection(stringValue));
            }

            populateElement(wspWriter, elm, object); // hook for more complex types
        }
        return elm;
    }

    /**
     * Should the value be serialized as an attribute value.
     *
     * <p>This implementation checks if the value contains information that
     * would be lost if serialized as an attribute.</p>
     *
     * @param value The value to be saved.
     * @return true if an attribute should be used
     */
    protected boolean serializeAsAttribute(String value) {
        boolean asAttr = true;

        if ( value != null ) {
            if ( value.indexOf('\r')>=0 || value.indexOf('\n')>=0 ) {
                asAttr = false;
            }
        }

        return asAttr;
    }

    /**
     * @return the namespace prefix to use for serialized elements created by this type mapping, ie "l7p31:".
     *         Includes the trailing colon if any.  Never null, but may be empty if the elements should be
     *         created in the default namespace.
     */
    protected String getNsPrefix() {
        return nsPrefix;
    }

    /** @return the namespace URI to use for serialized elements created by this type mapping, ie WspConstants.L7_POLICY_NS_31. */
    protected String getNsUri() {
        return nsUri;
    }

    /**
     * Do any extra work that might be requried by this element.
     *
     * @param wspWriter
     * @param newElement the newly-created element that needs to have properties filled in from object, whose
     *                   target may not be null.
     * @param object
     */
    protected void populateElement(WspWriter wspWriter, Element newElement, TypedReference object) throws InvalidPolicyTreeException {
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
            return doThaw(source, visitor);
        } catch (InvalidPolicyStreamException e) {
            try {
                return doThaw(visitor.invalidElement(source, e), visitor);
            } catch (NotNamedFormatException e1) {
                // Can't replace with UnknownAssertion, as it's an attribute in the middle of some other assertion
                throw new InvalidPolicyStreamException(ExceptionUtils.getMessage(e), e);
            }
        }
    }

    public TypeMappingFinder getSubtypeFinder() {
        // Basic types do not have subtypes
        return null;
    }

    private TypedReference doThaw(Element source, WspVisitor visitor)
            throws InvalidPolicyStreamException
    {
        if (TypeMappingUtils.findTypeName(source) == null)
            return thawAnonymous(source, visitor);
        return thawNamed(source, visitor);
    }

    protected TypedReference thawNamed(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        return doThawNamed(source, visitor, false);
    }

    protected TypedReference doThawNamed(Element source, WspVisitor visitor, boolean recursing) throws InvalidPolicyStreamException {
        String typeName = TypeMappingUtils.findTypeName(source);
        String value = source.getAttribute(typeName);

        if (typeName.endsWith("Null") && typeName.length() > 4) {
            typeName = typeName.substring(0, typeName.length() - 4);
            value = null;
            if (!isNullable) {
                final InvalidPolicyStreamException e = new InvalidPolicyStreamException("Policy contains a null " + externalName);
                if (recursing) throw e;
                return doThawNamed(visitor.invalidElement(source, e), visitor, true);
            }
        } else if ( typeName.endsWith("Reference") && typeName.length() > 9 ) {
            typeName = typeName.substring(0, typeName.length() - 9);
            value = DomUtils.getTextValue(source);
        }

        return doThawNamedNotNull(source, visitor, recursing, typeName, value);
    }

    protected TypedReference doThawNamedNotNull(Element source, WspVisitor visitor, boolean recursing, String typeName, String value) throws InvalidPolicyStreamException {
        if (!externalName.equals(typeName)) {
            final InvalidPolicyStreamException e = new InvalidPolicyStreamException("TypeMapping for " + clazz + ": unrecognized attr " + typeName);
            if (recursing) throw e;
            return doThawNamed(visitor.invalidElement(source, e), visitor, true);
        }

        if (value == null)
            return new TypedReference(clazz, null, source.getLocalName());

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
        TypedReference tr = new TypedReference(clazz, stringToObject(value), element.getLocalName());
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
        throw new NotNamedFormatException("BasicTypeMapping supports only Named format (Element: " + source.getLocalName() + ")");
    }

    /**
     * This method is responsible for constructing the newly deserialized object, but doesn't populate its fields.
     * For simple types, perform the reverse of objectToString.
     */
    protected Object stringToObject(String value) throws InvalidPolicyStreamException {
        if (stringConstructor == null)
            throw new InvalidPolicyStreamException("No stringToObject defined for TypeMapping for class " + clazz);
        try {
            //noinspection RedundantArrayCreation
            return stringConstructor.newInstance(new Object[]{value});
        } catch (Exception e) {
            throw new InvalidPolicyStreamException("Unable to convert string into " + clazz, e);
        }
    }
}
