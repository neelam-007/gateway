/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;

/**
 * Interface for something that knows how to convert an object of a given type into an XML element and vice versa.
 */
interface Pre32TypeMapping {
    /**
     * Get the concrete class that this Pre32TypeMapping recognizes.  A {@link Pre32TypedReference} passed to {@link #freeze}
     * must be of this type, and TypeReferences returned by {@link #thaw} will be of this type.
     *
     * @return the class that this Pre32TypeMapping supports, ie AllAssertion.class.  Never null.
     */
    Class getMappedClass();

    /**
     * Get the name that is used for this type inside serialized XML documents.
     *
     * @return  the name for this type, ie "All".  Never null.
     */
    String getExternalName();

    /**
     * Serialize the specified Object as a child element of the specified container element.  The serialized
     * element will be added to container with appendChild(), and will also be returned for reference.
     * <p/>
     * If object has a non-null name field, then "Named" format will be used for the returned Element:
     * the returned element will look like <code>&lt;name typeValueNull="null"/&gt;</code> if null, like
     * <code>&lt;name typeValue="..."/&gt;</code> if a non-null simple type, or like
     * <code>&lt;name typeValue="included"&gt;...&lt;/name&gt;</code> if a non-null complex type.
     * <p/>
     * If object has a null name field, then "Anonymous" format will be used for the returned Element:
     * the object may not be null, and the returned element will look like <code>&lt;Type&gt;...&lt;/Type&gt;</code>
     *
     * @param object    the object to serialize
     * @param container the container to receive it
     * @return the newly created Element, which has also been appended underneath container
     */
    Element freeze(Pre32TypedReference object, Element container);

    /**
     * De-serialize the specified XML element into an Object and return a Pre32TypedReference the new Object.
     * The returned Pre32TypedReference will have a name if one is known.
     *
     * @param source
     */
    Pre32TypedReference thaw(Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException;
}
