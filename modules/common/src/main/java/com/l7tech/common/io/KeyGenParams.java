package com.l7tech.common.io;

import java.io.Serializable;

/**
 * Parameters for generating a new key pair.
 * <p/>
 * Used for requests sent from the SSM since JCE spec classes are not serializable.
 */
public class KeyGenParams implements Serializable {
    public static final String ALGORITHM_RSA = "RSA";
    public static final String ALGORITHM_EC = "EC";

    // Key generation
    private String algorithm;
    private int keySize;
    private String namedParam;

    /**
     * Create an empty KeyGenParams.
     */
    public KeyGenParams() {
    }

    /**
     * Create a KeyGenParams that specifies an RSA key of the specified bit size.
     *
     * @param rsaKeyBits size of the RSA key in bits, ie 2048.
     */
    public KeyGenParams(int rsaKeyBits) {
        this.algorithm = ALGORITHM_RSA;
        this.keySize = rsaKeyBits;
    }

    /**
     * Create a KeyGenParams that specifies an EC key generated on the specified named curve.
     *
     * @param eccCurveName the named curve to use, ie "secp384r1".
     */
    public KeyGenParams(String eccCurveName) {
        this.algorithm = ALGORITHM_EC;
        this.namedParam = eccCurveName;
    }

    /**
     * @return the type of key to generate, ie "RSA" or "EC", or null to use the default (typically RSA).
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * @param algorithm the type of key to generate, ie "RSA" or "EC", or null to use the default (typically RSA).
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * @return the size of the key in bits (ie, 2048 for a 2048 bit RSA key), or 0 to use the default for this key type
     */
    public int getKeySize() {
        return keySize;
    }

    /**
     * Set key size in bits, if relevant for the intended key type.
     *
     * @param keySize the size of the key in bits (ie, 2048 for a 2048 bit RSA key), or 0 to use the default for this key type
     */
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    /**
     * @return the name set of key generation parameters (ie, a well known EC named curve like "secp384r1"), or null to use the default for this key type
     */
    public String getNamedParam() {
        return namedParam;
    }

    /**
     * Set named generation parameters, if relevant for the intended key type.
     *
     * @param namedParam the name set of key generation parameters (ie, a well known EC named curve like "secp384r1"), or null to use the default for this key type
     */
    public void setNamedParam(String namedParam) {
        this.namedParam = namedParam;
    }

    /**
     * Get the signature algorithm to use for the given key algorithm and hash.
     *
     * @param keyAlgorithm The key algorithm (e.g. RSA)
     * @param hashAlgorithm The hash algorithm
     * @return The algorithm or null if the key type is unknown or the hashAlgorithm was null
     */
    public static String getSignatureAlgorithm( final String keyAlgorithm,
                                                final String hashAlgorithm ) {
        String signatureAlgorithm = null;

        if ( hashAlgorithm != null ) {
            if ( ALGORITHM_RSA.equals( keyAlgorithm ) ) {
                signatureAlgorithm = hashAlgorithm + "withRSA";
            } else if ( ALGORITHM_EC.equals( keyAlgorithm ) ) {
                signatureAlgorithm = hashAlgorithm + "withECDSA";
            }
        }

        return signatureAlgorithm;
    }
}
