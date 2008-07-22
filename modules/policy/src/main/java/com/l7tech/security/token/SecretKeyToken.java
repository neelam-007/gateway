/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.token;

import com.l7tech.util.InvalidDocumentFormatException;

import java.security.GeneralSecurityException;

/**
 * @author mike
 */
public interface SecretKeyToken extends XmlSecurityToken {
    /**
     * Get the secret key from this token.
     *
     * @return  the secret key bytes from this token.  Never null.
     * @throws InvalidDocumentFormatException   if there was a problem lazily unwrapping this key
     * @throws GeneralSecurityException   if there was a problem lazily unwrapping this key
     */
    byte[] getSecretKey() throws InvalidDocumentFormatException, GeneralSecurityException;
}
