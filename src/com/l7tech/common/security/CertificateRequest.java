/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import java.security.PublicKey;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;

/**
 * Generic certificate request.  Opaque handle for typesafe use of JCE-specific CSR holders.
 * @author mike
 * @version 1.0
 */
public abstract class CertificateRequest {
    /**
     * @return a string like "cn=lyonsm"
     */
    public abstract String getSubjectAsString();

    /**
     * @return the bytes of the encoded form of this certificate request
     */
    public abstract byte[] getEncoded();

    /**
     * @return the public key in this certificate request
     */
    public abstract PublicKey getPublicKey() throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException;
}
