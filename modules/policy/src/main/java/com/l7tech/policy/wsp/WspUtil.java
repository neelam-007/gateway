/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.util.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility methods of use to WS-Policy type mappers
 */
class WspUtil {
    /**
     * Ensure that the specified element has wsp:Usage="wsp:Required" and any namespace declarations necessary
     * to support this.  This is complicated by the fact that some xpath expressions used in
     * MessagePredicate, Integrity/MessageParts and Confidentiality/MessageParts may use and hence have overridden
     * the "wsp" namespace prefix declared at the top of the policy.  This method checks for this and, if this is
     * the case, declares and uses an unused prefix such as "wsp1" for the Usage attribute (incrementing
     * the numeral until the prefix is unique).
     *
     * @param element        the element that should have wsp:Usage="wsp:Required"
     * @param wspPfxDesired  the desired starting prefix, ie "wsp".  Must not be null or empty.
     * @param wspUri         the desired WSP namespace uri, ie {@link WspConstants#WSP_POLICY_NS}.
     */
    static void setWspUsageRequired(Element element, final String wspPfxDesired, final String wspUri) {
        String wspPfx = wspPfxDesired;
        boolean wspDeclared = false;
        int suff = 1;
        Map nsMap = DomUtils.getNamespaceMap(element);
        for (Iterator i = nsMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String prefix = (String)entry.getKey();
            String uri = (String)entry.getValue();
            if (wspUri.equals(uri)) {
                wspPfx = prefix;
                wspDeclared = true;
                break;
            }
            if (wspPfx.equals(prefix))
                wspPfx = wspPfxDesired + suff++;
        }
        if (!wspDeclared)
            element.setAttributeNS(DomUtils.XMLNS_NS,  "xmlns:" + wspPfx, wspUri);
        element.setAttributeNS(wspUri, wspPfx + ":" + "Usage", wspPfx + ":Required");
    }

    private static Pattern FIND_REQUIRED = Pattern.compile("[#:]Required\\s*$");

    /**
     * Enforce that the specified element either does not have an attribute with the localName "Usage", or if it does,
     * that the value ends with either "#Required" or ":Required".
     *
     * @param element the element to enforce
     * @throws InvalidPolicyStreamException if the element
     */
    static void enforceWspUsageRequired(Element element) throws InvalidPolicyStreamException {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr)attrs.item(i);
            if ("Usage".equals(attr.getLocalName())) {
                String value = attr.getValue();
                if (!FIND_REQUIRED.matcher(value).find())
                    throw new InvalidPolicyStreamException("Element " + element.getNodeName() + " has unsupported wsp:Usage of " + value);
            }
        }
    }
}
