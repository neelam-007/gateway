package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.JsonWebSignature;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.AVAILABLE_SECRET_NONE;

/**
 * These modules only sign and validate signatures on data using their
 * respective signature algorithms.  They DO NOT validate JSON or any other
 * such nonsense...just sign JWT secured input and validate JWT token signatures.
 * <p/>
 * User: rseminoff
 * Date: 30/11/12
 */
public class JwsNone extends JsonWebSignature {

    public static final String jwsAlgorithmName = "none";

    public JwsNone() {
        super(JwsNone.jwsAlgorithmName, "No Signature", "", logger, AVAILABLE_SECRET_NONE);
    }

    public byte[] signData(byte[] header, byte[] payload) {
        return new byte[0];  // No signature to generate.
    }

    public boolean validateToken(byte[] header, byte[] payload, byte[] signature) {
        return true;    // No validation needed, as there's no signature to compare.
    }

    @Override
    public int compareTo(Object o) {
        return -1;  // None needs to always be first in any list.
    }
}
