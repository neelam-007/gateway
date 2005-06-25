/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * @author mike
 */
public class MessagePredicateMapping extends AssertionMapping {
    private static final String DIALECT_XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";

    MessagePredicateMapping(Assertion a, String externalName) {
        super(a, externalName);
    }

    public MessagePredicateMapping(Assertion a, String externalName, String nsUri, String nsPrefix) {
        super(a, externalName, nsUri, nsPrefix);
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        if (wspWriter.isPre32Compat())
            return super.freeze(wspWriter, object, container);

        // Build a wsp:MessagePredicate

        XpathBasedAssertion ass = (XpathBasedAssertion)object.target;
        if (ass == null) throw new InvalidPolicyTreeException("Missing XpathBasedAssertion"); // can't happen
        Map nsMap = ass.getXpathExpression().getNamespaces();

        // Find a wsp ns prefix that won't collide with one of the decls we need to add
        String wspPfx = "wsp";
        String wspUri = WspConstants.WSP_POLICY_NS;
        int i = 1;
        while (nsMap.keySet().contains(wspPfx) && !wspUri.equals(nsMap.get(wspPfx)))
            wspPfx = "wsp" + i++;

        // Create the MessagePredicate element
        // TODO: There is no way to prevent some "extra" decls from being in scope (ie, wsp: and l7p:)
        //       Extra decls change the meaning of the xpath.  This is bad!  For now we will ignore the problem
        //       at least we won't define a wsp: decl that collides with a wsp: decl used by the xpath.
        Element messagePredicate = XmlUtil.createAndAppendElementNS(container, "MessagePredicate", wspUri, wspPfx);
        messagePredicate.setAttributeNS(wspUri, wspPfx + ":" + "Usage", wspPfx + ":Required");
        messagePredicate.setAttribute("Dialect", DIALECT_XPATH);
        addNsDeclsToElement(messagePredicate, nsMap);
        messagePredicate.appendChild(XmlUtil.createTextNode(messagePredicate, ass.getXpathExpression().getExpression()));
        return messagePredicate;
    }

    private void addNsDeclsToElement(final Element element, final Map nsMap) {
        Collection nsDecls = nsMap.entrySet();
        for (Iterator i = nsDecls.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            if (entry.getKey() != null && entry.getKey().toString().length() > 0)
                element.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:" + entry.getKey(), entry.getValue().toString());
        }
    }

    public TypedReference thaw(Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        if (!"MessagePredicate".equals(source.getLocalName()))
            return super.thaw(source, visitor); // handle old-school format

        // Parse a MessagePredicate assertion
        // TODO either support wsp:Usage other than Required here, or remove it from freeze.  (It's no longer in the spec anyway)
        String dialect = source.getAttribute("Dialect");
        if (dialect != null && !DIALECT_XPATH.equals(dialect))
            throw new InvalidPolicyStreamException("Unsupported MessagePredicate Dialect: " + dialect);
        String xpathExpr = XmlUtil.getTextValue(source);
        Map nsMap = XmlUtil.getNamespaceMap(source);
        XpathBasedAssertion ass = new RequestXpathAssertion(new XpathExpression(xpathExpr, nsMap));
        return new TypedReference(RequestXpathAssertion.class, ass);
    }
}
