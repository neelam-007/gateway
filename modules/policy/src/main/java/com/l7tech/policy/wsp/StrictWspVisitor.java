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
    private final TypeMappingFinder typeMappingFinder;

    public StrictWspVisitor(TypeMappingFinder typeMappingFinder) {
        this.typeMappingFinder = typeMappingFinder;
    }

    public void unknownProperty(Element originalObject,
                                Element problematicParameter,
                                Object deserializedObject,
                                String parameterName,
                                TypedReference parameterValue,
                                Throwable problemEncountered)
            throws InvalidPolicyStreamException
    {
        throw new InvalidPolicyStreamException("Unknown property " + parameterName + " of " + deserializedObject.getClass(), problemEncountered);
    }

    public void propertyTypeMismatch(Element originalObject,
                                     Element problematicParameter,
                                     Object deserializedObject,
                                     String parameterName,
                                     TypedReference parameterValue,
                                     Throwable problemEncountered)
            throws InvalidPolicyStreamException
    {
        assert false: "Mismatch property type for " + parameterName + " of " + deserializedObject.getClass() ;
    }

    public Element invalidElement(Element problematicElement, Exception problemEncountered) throws InvalidPolicyStreamException {
        throw new InvalidPolicyStreamException("Unrecognized element " + problematicElement.getLocalName(), problemEncountered);
    }

    public TypeMappingFinder getTypeMappingFinder() {
        return typeMappingFinder;
    }
}
