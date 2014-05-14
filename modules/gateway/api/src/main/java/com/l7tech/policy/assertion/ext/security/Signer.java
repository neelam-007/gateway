package com.l7tech.policy.assertion.ext.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/**
 * Provides methods to create a signature.
 */
public interface Signer {
    /**
     * Create a signature using this signer's key using the specified hash algorithm and data to sign.
     *
     * @param hashAlgorithmName a hash algorithm name, eg "SHA-256".  This will be combined with the current private key
     *                          type to produce a full signature algorithm ("SHA256withECDSA" or "SHA256withRSA").
     *                          If null, will default to "SHA-1".
     * @param dataToSign data to feed to the signature to be signed.  Required.
     * @return a DER-encoded signature as returned by the JCE Signature implementation
     * @throws NoSuchAlgorithmException if the signature algorithm name is invalid or not available
     * @throws InvalidKeyException if the key is invalid
     * @throws SignatureException if signature can't be created
     * @throws IOException if data can't be read
     */
    byte[] createSignature(String hashAlgorithmName, InputStream dataToSign)
        throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException;
}