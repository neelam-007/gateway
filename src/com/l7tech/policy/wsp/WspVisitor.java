/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

/**
 * Interface implemented by callers who desire more control over the behaviour of WspReader when unrecognized
 * elements/assertions/attributes are encountered during parsing.
 */
interface WspVisitor {
    /**
     * Report a problem setting a parameter from the serialized policy.
     *
     * @param originalObject       the entire object's source XML.
     * @param problematicParameter the source XML for the particular parameter we had trouble dealing with.   
     * @param deserializedObject   object whose property we were trying to set.  Never null.
     * @param parameterName        name of the parameter we had trouble setting.  Never null.
     * @param parameterValue       value we were trying to set it to.  Never null, but its target might be.
     * @param problemEncountered   Exception that was encountered while trying to set the property.  Might be null.
     * @throws InvalidPolicyStreamException  if the visitor was unable to handle the unknown property to its satisfaction
     */ 
    void unknownProperty(Element originalObject, 
                         Element problematicParameter,
                         Object deserializedObject, 
                         String parameterName, 
                         TypedReference parameterValue,
                         Exception problemEncountered)
            throws InvalidPolicyStreamException;
    
    /**
     * Report a problem parsing an element in the serialized policy.  Visitor can optionally return an alternate
     * element to use instead.
     * 
     * @param problematicElement the element that we were unable to process.  never null.
     * @param problemEncountered the exception we encountered, if any. may be null.
     * @return a new Element to use in place of the problematic one.  never null.
     * @throws InvalidPolicyStreamException if a replacement element could not be constructed.
     */ 
    Element invalidElement(Element problematicElement,
                           Exception problemEncountered)
            throws InvalidPolicyStreamException;
}
