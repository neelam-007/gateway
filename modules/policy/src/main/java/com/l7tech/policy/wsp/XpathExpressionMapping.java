/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.util.DomUtils;
import com.l7tech.xml.xpath.XpathExpression;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Mapping for freezing xpaths into elements that include xmlns: declarations for each qname in the expression,
 * such as MessagePredicate, Integrity/MessageParts, and Confidentiality/MessageParts.
 */
class XpathExpressionMapping implements TypeMapping {
    private static final String DIALECT_XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";
    private final String elementName; // element name, ie "MessageParts"
    private final String nsUri;
    private final String desiredPrefix;

    XpathExpressionMapping(String elementName, String nsUri, String desiredPrefix) {
        this.elementName = elementName;
        this.nsUri = nsUri;
        this.desiredPrefix = desiredPrefix;
    }


    public Class getMappedClass() {
        return XpathExpression.class;
    }

    public String getExternalName() {
        return elementName;
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (wspWriter.isPre32Compat())
            throw new InvalidPolicyTreeException("XpathExpressionMapping cannot be used in pre32 compat mode");
        if (object.target == null)
            throw new InvalidPolicyTreeException("XpathExpressionMapping can't freeze null object");
        if (!(object.target instanceof XpathExpression))
            throw new InvalidPolicyTreeException("XpathExpressionMapping can't freeze object of type " + object.target.getClass());

        // Build the element

        XpathExpression xpathExpression = (XpathExpression)object.target;
        if (xpathExpression == null) throw new InvalidPolicyTreeException("Missing XpathExpression"); // can't happen
        Map nsMap = xpathExpression.getNamespaces();

        // Find a wsp ns prefix that won't collide with one of the decls we need to add
        String wspUri = WspConstants.WSP_POLICY_NS;
        String wspPfx = findUnusedPrefix("wsp", wspUri, nsMap);

        // Find an ns prefix for our own element that won't collide with one of the decls we need to add
        String myPfx = findUnusedPrefix(desiredPrefix, nsUri, nsMap);

        // Find the namespace declarations already in scope
        Map nsAlready = DomUtils.getNamespaceMap(container);

        // Create the MessagePredicate element
        //   There is no way to prevent some "extra" decls from being in scope (ie, wsp: and l7p:)
        //   This should not be a problem, since according to XPath 1.0, if the expression contained prefixes
        //   not bound to any namespace it was not a valid expression in the first place.
        Element element = DomUtils.createAndAppendElementNS(container, elementName, nsUri, myPfx);
        element.setAttribute("Dialect", DIALECT_XPATH);
        addNsDeclsToElement(element, nsMap, nsAlready);
        element.appendChild(DomUtils.createTextNode(element, xpathExpression.getExpression()));
        return element;
    }

    private String findUnusedPrefix(String initialPfx, String wspUri, Map nsMap) {
        String wspPfx = initialPfx;
        int i = 1;
        while (nsMap.keySet().contains(wspPfx) && !wspUri.equals(nsMap.get(wspPfx)))
            wspPfx = initialPfx + i++;
        return wspPfx;
    }

    /**
     * Add xmlns:blah="urn:foo" delcarations to the specified element for each declration in nsMap, except those
     * which already have identical declarations present in nsAlready.
     *
     * @param element  the element the decorate
     * @param nsMap    the namespace declarations that must be valid inside element
     * @param nsAlready the namespace declarations that are already in-scope for element
     */
    private void addNsDeclsToElement(final Element element, final Map nsMap, Map nsAlready) {
        Collection nsDecls = nsMap.entrySet();
        for (Iterator i = nsDecls.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            if (key == null || value == null || key.length() < 1)
                continue; // not a valid prefixed namespace decl
            if (value.equals(nsAlready.get(key)))
                continue; // this exactly declaration is already in scope; no need to duplicate it
            element.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:" + entry.getKey(), entry.getValue().toString());
        }
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        // Parse a MessagePredicate, Integrity/MessageParts, or Confidentiality/MessageParts assertion
        if (source == null) throw new InvalidPolicyStreamException("Missing XPath based assertion element (expecting " + elementName + ")");
        String dialect = source.getAttribute("Dialect");
        if (dialect != null && dialect.length() > 0 && !DIALECT_XPATH.equals(dialect))
            throw new InvalidPolicyStreamException("Unsupported MessagePredicate Dialect: " + dialect);
        String xpathExpr = DomUtils.getTextValue(source);
        Map nsMap = DomUtils.getNamespaceMap(source);
        final XpathExpression xpathExpression = new XpathExpression(xpathExpr, nsMap);
        return new TypedReference(XpathExpression.class, xpathExpression);
    }

    public TypeMappingFinder getSubtypeFinder() {
        return null;
    }
}
