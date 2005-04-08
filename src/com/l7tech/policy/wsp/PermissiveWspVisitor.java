/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
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
        UnknownAssertion ua;
        try {
            ua = new UnknownAssertion(problemEncountered,
                                      XmlUtil.nodeToString(problematicElement));
        } catch (IOException e) {
            throw new RuntimeException("Unable to encapsulate invalid element", e); // shouldn't happen
        }
        Element node = WspWriter.toElement(ua);
        return (Element)problematicElement.getOwnerDocument().importNode(node, true);
    }
}
