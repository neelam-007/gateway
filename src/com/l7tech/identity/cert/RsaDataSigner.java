/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.cert;

import java.security.interfaces.RSAPrivateKey;
import java.security.Signature;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

/**
 * Sign and verify arbitrary data using RSA public keys.
 */
public class RsaDataSigner {
    private RsaDataSigner() {}

    /**
     * Sign the specified bytes with the specified RSA private key, using the SHA1withRSA signature algorithm,
     * and return the resulting signature as a byte array.
     *
     * @param data          the data to sign
     * @param privateKey    the private key to sign it with
     * @return              the computed signature
     * @throws RuntimeException     if the SHA1withRSA signature algorithm is not available
     * @throws InvalidKeyException  if the specified RSAPrivateKey is not valid
     */
    public static byte[] sign(byte[] data, RSAPrivateKey privateKey) throws InvalidKeyException {
        try {
            Signature signer = Signature.getInstance(SIG_ALG);
            signer.initSign(privateKey);
            signer.update(data);
            return signer.sign();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to access signature algorithm " + SIG_ALG, e); // shouldn't happen
        } catch (SignatureException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Validate that private key corresponding to the specified certifcate was used to produce the specified
     * signature of the specified data bytes.
     * @param data              that signed data
     * @param signature         the signature
     * @param signerRsaCert     the certificate containing the public key corresponding to the RSA private key
     *                          that was used to generate the signature
     * @return true if the signature is valid; otherwise, false
     * @throws InvalidKeyException if the specified certificate does not contain a valid RSA public key
     */
    public static boolean validateSignature(byte[] data, byte[] signature, X509Certificate signerRsaCert)
            throws InvalidKeyException
    {
        try {
            Signature verifier = Signature.getInstance(SIG_ALG);
            verifier.initVerify(signerRsaCert);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to access signature algorithm " + SIG_ALG, e); // shouldn't happen
        } catch (SignatureException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private static final String SIG_ALG = "SHA1withRSA";
}
