package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.ecdsa;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.JsonWebSignature;

import java.util.logging.Logger;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.AVAILABLE_SECRET_VARIABLE;

/**
 * User: rseminoff
 * Date: 09/01/13
 */
public abstract class JwsEcdsa extends JsonWebSignature {

    /**
     * This will contain future sign/validate methods when ready to implement elliptical curve signing.
     */
    public JwsEcdsa(String algorithmName, String description, String javaAlgorithm, Logger logger) {
        // This will need to be updated when EC is implemented.
        super(algorithmName, description, javaAlgorithm, logger, AVAILABLE_SECRET_VARIABLE);
    }

    /**
     * This is called to sign digital data with the passed Private Key.
     * All parameters should be plaintext/plainbyte, and not Base64URL encoded.
     * @param header The JSON Header as a byte array.
     * @param payload The JSON Payload as a byte array
     * @param ? The elliptical cure data required to sign.  This is x, y, and private d.
     */
//    public byte[] signData(byte[] header, byte[] payload, ... ) {}

    /**
     * Validates the passed token using the passed Public Key.
     * All parameters should be Base64URLEncoded, except secret, which is a PublicKey
     * @param header The JSON Header as a byte array.
     * @param payload The JSON Payload as a byte array
     * @param signature The signature as a byte array
     * @param ? The elliptical data required to validate.  This is x, y, R and S.
     */
//    public boolean validateToken(byte[] header, byte[] payload, byte[] signature, PublicKey secret) {}

}
