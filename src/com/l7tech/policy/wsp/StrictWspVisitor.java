/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

/**
 * Implementation of WspVisitor that aborts policy parsing whenever it encounters any error.
 */
class StrictWspVisitor implements WspVisitor {
    /** A WspVisitor that always throws if any problem is encountered. */
    static final WspVisitor INSTANCE = new StrictWspVisitor();

    public void unknownProperty(Element originalObject,
                                Element problematicParameter,
                                Object deserializedObject,
                                String parameterName,
                                TypedReference parameterValue,
                                Exception problemEncountered)
            throws InvalidPolicyStreamException
    {
        throw new InvalidPolicyStreamException("Unknown property " + parameterName + " of " + deserializedObject.getClass(), problemEncountered);
    }

    public Element invalidElement(Element problematicElement, Exception problemEncountered) throws InvalidPolicyStreamException {
        throw new InvalidPolicyStreamException("Unrecognized element " + problematicElement.getLocalName(), problemEncountered);
    }
}
