/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import com.l7tech.util.DomUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.UnknownAssertion;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of Pre32WspVisitor that aborts policy parsing whenever it encounters any error.
 */
class Pre32WspVisitor {
    private static final Logger logger = Logger.getLogger(Pre32WspVisitor.class.getName());

    /** Log the unknown property and continue. */
    public void unknownProperty(Element originalObject,
                                Element problematicParameter,
                                Object deserializedObject,
                                String parameterName,
                                Pre32TypedReference parameterValue,
                                Exception problemEncountered)
            throws Pre32InvalidPolicyStreamException
    {
        logger.log(Level.FINE, "Ignoring invalid property " + parameterName + " of " + deserializedObject.getClass());
    }

    /** Replace the problematic element with an UnknownAssertion. */
    public Element invalidElement(Element problematicElement,
                                  Exception problemEncountered)
            throws Pre32InvalidPolicyStreamException
    {
        if ("UnknownAssertion".equals(problematicElement.getLocalName()))
            throw new Pre32InvalidPolicyStreamException("Unable to handle invalid UnknownAssertion");
        String elName = problematicElement.getLocalName();
        if (elName == null || elName.length() < 1) elName = problematicElement.getNodeName();
        UnknownAssertion ua;
        ua = UnknownAssertion.create(elName, null);
        Element node = Pre32WspWriter.toElement(ua);
        try {
            // Preserve original XML, if possible, but only after encapsulating it
            final String originalXml = XmlUtil.nodeToString(problematicElement);
            Element oxNode = DomUtils.createAndAppendElementNS(node, "OriginalXml", node.getNamespaceURI(), "p");
            oxNode.setAttribute("stringValue", originalXml);
        } catch (IOException e) {
            throw new RuntimeException("Unable to encapsulate invalid element", e); // shouldn't happen
        }
        return (Element)problematicElement.getOwnerDocument().importNode(node, true);
    }
}
