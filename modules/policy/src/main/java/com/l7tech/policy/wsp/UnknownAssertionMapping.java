/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.util.DomUtils;
import com.l7tech.util.TooManyChildElementsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Mapping specifically for {@link UnknownAssertion}.  Behaves normally when parsing one.  When saving one
 * into a policy, however, first attempts to preserve the original XML if there was any and it consisted
 * of a well-formed XML snippet consisting of a single element.
 */
class UnknownAssertionMapping extends AssertionMapping {
    public UnknownAssertionMapping() {
        super(new UnknownAssertion(), "UnknownAssertion");
    }

    public Element freeze(WspWriter wspWriter, TypedReference object, Element container) {
        // First try to preserve the original XML, if there is some and it's a well-formed fragment
        // consisting of a single element.
        UnknownAssertion ua = (UnknownAssertion)object.target;
        try {
            if (ua != null && ua.getOriginalXml() != null && ua.getOriginalXml().length() >= 1) {
                Document doc = wspWriter.createSkeleton("DUMMY", ua.getOriginalXml());
                Element originalElement = DomUtils.findOnlyOneChildElement(doc.getDocumentElement());
                if (originalElement != null) {
                    Node imported = container.getOwnerDocument().importNode(originalElement, true);
                    container.appendChild(imported);
                    return (Element)imported;
                }
            }
        } catch (SAXException | TooManyChildElementsException e) {
            // fall through and just serialized the UA
        }
        return super.freeze(wspWriter, object, container);
    }
}
