/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.policy.assertion.UnknownAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Mapping specifically for {@link UnknownAssertion}.  Behaves normally when parsing one.  When saving one
 * into a policy, however, first attempts to preserve the original XML if there was any and it consisted
 * of a well-formed XML snippet consisting of a single element.
 */
class UnknownAssertionMapping extends AssertionMapping {
    public UnknownAssertionMapping() {
        super(new UnknownAssertion(), "UnknownAssertion");
    }

    public Element freeze(TypedReference object, Element container) {
        // First try to preserve the original XML, if there is some and it's a well-formed fragment
        // consisting of a single element.
        UnknownAssertion ua = (UnknownAssertion)object.target;
        try {
            if (ua != null && ua.getOriginalXml() != null && ua.getOriginalXml().length() >= 1) {
                Document doc = XmlUtil.stringToDocument("<DUMMY>" + ua.getOriginalXml() + "</DUMMY>");
                Element originalElement = XmlUtil.findOnlyOneChildElement(doc.getDocumentElement());
                if (originalElement != null) {
                    Node imported = container.getOwnerDocument().importNode(originalElement, true);
                    container.appendChild(imported);
                    return (Element)imported;
                }
            }
        } catch (IOException e) {
            // fall through and just serialized the UA
        } catch (SAXException e) {
            // fall through and just serialized the UA
        } catch (TooManyChildElementsException e) {
            // fall through and just serialized the UA
        }
        return super.freeze(object, container);
    }
}
