/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;



/**
 * @author alex
 * @version $Revision$
 */
public interface SigningSecurityToken extends SecurityToken {
    /**
     * @return true if the sender has proven its possession of the private key corresponding to this security token.
     * This is done by signing one or more elements of the message with it.
     */
    boolean isPossessionProved();

    /**
     * @return An array of elements signed by this signing security token.  May be empty but never null.
     */
    SignedElement[] getSignedElements();
}
