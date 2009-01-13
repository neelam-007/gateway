/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.token;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author mike
 */
public interface UsernameToken extends XmlSecurityToken, HasUsernameAndPassword {
    /** @return XML serialized version of this SecurityToken using the specified Security namespace and owner document. */
    Element asElement(Document factory, String securityNs, String securityPrefix);

    /**
     * @return XML serialized version of this SecurityToken.  This will return an existing element, if there is one.
     *         Otherwise, a new element will be created as the root of a new document and returned.
     *         This will use the default security namespace and prefix.
     */
    Element asElement();

    /**
     * Get the nonce for this token, if any.
     *
     * @return the Nonce from this token as a Base64-encoded byte array, or null.
     */
    String getNonce();

    /**
     * Get the creation date for this token, if any.
     *
     * @return the created date as a String in ISO 8601 format, or null.
     */
    String getCreated();

    /**
     * Check if this username token contains a digested password.
     *
     * @return true if {@link #getPasswordDigest()} would return non-null.
     */
    boolean isDigest();

    /**
     * Get the password digest found in this username token, if any.
     *
     * @return the Base64-encoded digested password, or null.
     */
    String getPasswordDigest();
}
