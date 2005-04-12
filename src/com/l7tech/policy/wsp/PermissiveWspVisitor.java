/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.UnknownAssertion;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WspVisitor implementation that ignores unknown properties, and tries to preserve unknown elements as UnknownAssertions.
 */
class PermissiveWspVisitor implements WspVisitor {
    private static final Logger logger = Logger.getLogger(PermissiveWspVisitor.class.getName());
    public static final WspVisitor INSTANCE = new PermissiveWspVisitor();

    /** Log the unknown property and continue. */
    public void unknownProperty(Element originalObject,
                                Element problematicParameter,
                                Object deserializedObject,
                                String parameterName,
                                TypedReference parameterValue,
                                Exception problemEncountered)
            throws InvalidPolicyStreamException
    {
        logger.log(Level.FINE, "Ignoring invalid property " + parameterName + " of " + deserializedObject.getClass());
    }

    /** Replace the problematic element with an UnknownAssertion. */
    public Element invalidElement(Element problematicElement,
                                  Exception problemEncountered)
            throws InvalidPolicyStreamException
    {
        if ("UnknownAssertion".equals(problematicElement.getLocalName()))
            throw new InvalidPolicyStreamException("Unable to handle invalid UnknownAssertion");
        String elName = problematicElement.getLocalName();
        if (elName == null || elName.length() < 1) elName = problematicElement.getNodeName();
        UnknownAssertion ua;
        ua = UnknownAssertion.create(elName, null);
        Element node = WspWriter.toElement(ua);
        try {
            // Preserve original XML, if possible, but only after encapsulating it
            final String originalXml = XmlUtil.nodeToString(problematicElement);
            Element oxNode = XmlUtil.createAndAppendElementNS(node, "OriginalXml", node.getNamespaceURI(), "p");
            oxNode.setAttribute("stringValue", originalXml);
        } catch (IOException e) {
            throw new RuntimeException("Unable to encapsulate invalid element", e); // shouldn't happen
        }
        return (Element)problematicElement.getOwnerDocument().importNode(node, true);
    }
}
