/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import org.apache.ws.policy.PrimitiveAssertion;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Superclass for something that visits each node in a WS-SP subtree and gathers configuration state.
 */
abstract class WsspVisitor {
    static final String IS_REQUEST = "isRequest";
    private final WsspVisitor parent;

    protected WsspVisitor(WsspVisitor parent) {
        this.parent = parent;
    }

    /** @return the parent context for this context. */
    public WsspVisitor getParent() {
        return parent;
    }

    /** @return a map of element local name => PrimitiveAssertionConverter valid in this visitor's context. */
    protected abstract Map getConverterMap();

    /** Convert the specified Apache assertion tree into a Layer 7 assertion tree in this visitor's context. */
    com.l7tech.policy.assertion.Assertion recursiveConvert(org.apache.ws.policy.Assertion p) throws PolicyConversionException {
        switch (p.getType()) {
            case org.apache.ws.policy.Assertion.COMPOSITE_POLICY_TYPE:
                return recursiveConvertPolicy(p);

            case org.apache.ws.policy.Assertion.COMPOSITE_AND_TYPE:
                return recursiveConvertAll(p);

            case org.apache.ws.policy.Assertion.COMPOSITE_XOR_TYPE:
                return recursiveConvertXor(p);

            case org.apache.ws.policy.Assertion.PRIMITIVE_TYPE:
                return recursiveConvertPrimitive((org.apache.ws.policy.PrimitiveAssertion)p);

            case org.apache.ws.policy.Assertion.POLIY_REFERCE_TYPE:
                throw new PolicyConversionException("Unresolved policy reference encountered");

            default:
                throw new PolicyConversionException("Unknown assertion type: " + p.getType() + " (" + p.getClass().getName() + ")");
        }
    }

    /** Replace empty composite assertion with null, or singleton composite with it's single child. */
    static Assertion collapse(Assertion in) {
        return Assertion.simplify(in, false);
    }

    protected Assertion recursiveConvertPolicy(org.apache.ws.policy.Assertion p) throws PolicyConversionException {
        AllAssertion all = new AllAssertion();
        for (Iterator i = p.getTerms().iterator(); i.hasNext();) {
            final Assertion converted = collapse(recursiveConvert((org.apache.ws.policy.Assertion)i.next()));
            if (converted != null)
                all.addChild(converted);
        }
        return collapse(all);
    }

    protected Assertion recursiveConvertXor(org.apache.ws.policy.Assertion p) throws PolicyConversionException {
        ExactlyOneAssertion one = new ExactlyOneAssertion();
        for (Iterator i = p.getTerms().iterator(); i.hasNext();) {
            final Assertion converted = collapse(recursiveConvert((org.apache.ws.policy.Assertion)i.next()));
            if (converted != null)
                one.addChild(converted);
        }
        return collapse(one);
    }

    protected Assertion recursiveConvertAll(org.apache.ws.policy.Assertion p) throws PolicyConversionException {
        AllAssertion all = new AllAssertion();
        for (Iterator i = p.getTerms().iterator(); i.hasNext();) {
            final Assertion converted = collapse(recursiveConvert((org.apache.ws.policy.Assertion)i.next()));
            if (converted != null)
                all.addChild(converted);
        }
        return collapse(all);
    }

    /**
     * Convert the specified primitive assertion into a layer 7 assertion tree in this visitor's context.
     * <p/>
     * This method just does a lookup in {@link #getConverterMap}.
     */
    protected Assertion recursiveConvertPrimitive(org.apache.ws.policy.PrimitiveAssertion p) throws PolicyConversionException {
        final QName qname = p.getName();
        if (!"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy".equals(qname.getNamespaceURI()))
            throw new PolicyConversionException("Unrecognized primitive assertion (namespace URI is not http://schemas.xmlsoap.org/ws/2005/07/securitypolicy): " + qname);
        final String name = qname.getLocalPart();

        PrimitiveAssertionConverter converter = (PrimitiveAssertionConverter)getConverterMap().get(name);
        if (converter == null)
            throw new PolicyConversionException("Element not recognized this this context: " + p.getName());
        return collapse(converter.convert(this, p));
    }

    protected Assertion gatherPropertiesFromSubPolicy(PrimitiveAssertion p) throws PolicyConversionException {
        org.apache.ws.policy.Assertion term = p;
        QName name = p.getName();

        term = moveDown(term, name);
        if (term.getType() != org.apache.ws.policy.Assertion.COMPOSITE_POLICY_TYPE)
            throw new PolicyConversionException("Assertion does not contain a wsp:Policy element");

        term = moveDown(term, name);
        if (term.getType() != org.apache.ws.policy.Assertion.COMPOSITE_XOR_TYPE)
            throw new PolicyConversionException("Assertion does not contain a wsp:Policy/wsp:ExactlyOne element (policy not normalized?)");

        term = moveDown(term, name);
        if (term.getType() != org.apache.ws.policy.Assertion.COMPOSITE_AND_TYPE)
            throw new PolicyConversionException("Assertion does not contain a wsp:Policy/wsp:ExactlyOne/wsp:All element (policy not normalized?)");

        // Gather all properties from the All and its children.  Can assume that there is only one choice left since policy is normalized
        return recursiveConvert(term);
    }

    private org.apache.ws.policy.Assertion moveDown(org.apache.ws.policy.Assertion term, QName name) throws PolicyConversionException {
        List nested;
        nested = term.getTerms();
        if (nested == null) throw new PolicyConversionException("Assertion lacks normalized sub policy: " + name);
        if (nested.size() != 1) throw new PolicyConversionException("Assertion does not have exactly one normalized sub policy: " + name);
        term = (org.apache.ws.policy.Assertion)nested.get(0);
        return term;
    }

    /**
     * Callback to set a complex property.  p points at the element that names the property and that contains
     * a normalized wsp:Policy subelement containing the allowed property values.
     *
     * @param p the element representing the property to set and its new complex value.  Must not be null.
     */
    protected void setComplexProperty(PrimitiveAssertion p) throws PolicyConversionException {
        gatherPropertiesFromSubPolicy(p);
    }

    /**
     * Add a property value, assuming the property name is recognized.
     * <p/>
     * This method always delegates to the parent WsspVisitor, if any, and returns false if there is no parent.
     *
     * @param propName  the name of the propety to add a value to
     * @param propValue  the value, as a simple qname, to add to this property
     * @return  true if the property was recognized by this or a parent visitor and set successfully
     */
    protected boolean maybeAddPropertyQnameValue(String propName, QName propValue) {
        return false;
    }

    /**
     * Add a property value to this context.  Throws if the property was not recognized by this visitor or any parent.
     * <p/>
     * This method always delegates to the maybeAddPropertyQnameValue, and throws if no parent is able to accept the property.
     *
     * @param propName  the name of the propety to add a value to
     * @param propValue  the value, as a simple qname, to add to this property
     * @throws PolicyConversionException if the proeprty was not recognized by this or a parent visitor
     */
    public final void addPropertyQnameValue(String propName, QName propValue) throws PolicyConversionException {
        if (!maybeAddPropertyQnameValue(propName, propValue)) {
            if (parent != null)
                parent.addPropertyQnameValue(propName, propValue);
            else
                throw new PolicyConversionException("Property unrecognized in this context: " + propName + " (value=" + propValue + ")");
        }
    }

    protected boolean maybeSetSimpleProperty(String propName, boolean propValue) {
        return false;
    }

    /**
     * Set a simple property value in this context.  Throws if the property was not recognized by this visitor or any
     * parent.
     * <p/>
     *
     * @param propName
     * @param propValue
     */
    public final void setSimpleProperty(String propName, boolean propValue) throws PolicyConversionException {
        if (!maybeSetSimpleProperty(propName, propValue)) {
            if (parent != null)
                parent.setSimpleProperty(propName, propValue);
            else
                throw new PolicyConversionException("Boolean property unrecognized in this context: " + propName + " (value=" + propValue + ")");
        }
    }

    public boolean isSimpleProperty(String propName) throws PolicyConversionException {
        if (parent != null) return parent.isSimpleProperty(propName);
        throw new PolicyConversionException("Unrecognized property: " + propName);
    }
}
