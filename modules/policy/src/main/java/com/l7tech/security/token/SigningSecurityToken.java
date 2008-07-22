/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.token;



/**
 * @author alex
 * @version $Revision$
 */
public interface SigningSecurityToken extends XmlSecurityToken {
    /**
     * @return true if the sender has proven its possession of the private key corresponding to this security token.
     * This is done by signing one or more elements of the message with it.
     */
    boolean isPossessionProved();

    /**
     * @return An array of elements signed by this signing security token.  May be empty but never null.
     */
    SignedElement[] getSignedElements();

    /**
     * Record that a particular element was found with a valid signature in the current message, using a signature
     * key that ultimately came from this token.  Typically this method should not be used outside of the WssProcessor
     * implementation.
     *
     * @param signedElement the signed element to record.  Must not be null.
     */
    void addSignedElement(SignedElement signedElement);

    /**
     * @return An array of parts signed by this signing security token.  May be empty but never null.
     */
    SignedPart[] getSignedParts();

    /**
     * Record that a particular part was found with a valid signature in the current message, using a signature
     * key that ultimately came from this token.  Typically this method should not be used outside of the WssProcessor
     * implementation.
     *
     * @param signedPart the signed part to record.  Must not be null.
     */
    void addSignedPart(SignedPart signedPart);

    /**
     * Record that the sender has proven its possession of the private key corresponding to this security token.
     * Typically this method should not be used outside of the WssProcessor. 
     */
    void onPossessionProved();
}
