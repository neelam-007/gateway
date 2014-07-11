package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.hmac;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.JsonWebSignature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.*;

/**
 * User: rseminoff
 * Date: 09/01/13
 */
public abstract class JwsHmac extends JsonWebSignature {

    private static final Logger logger = Logger.getLogger(JwsHmac.class.getName());

    public JwsHmac(String algorithmName, String description, String javaAlgorithm, Logger logger) {
        super(algorithmName, description, javaAlgorithm, logger, AVAILABLE_SECRET_PASSWORD + AVAILABLE_SECRET_VARIABLE + AVAILABLE_SECRET_VARIABLE_BASE64);
    }

    /**
     * This is called to sign digital data with the passed secret.
     * All parameters should be plaintext/plainbyte, and not Base64URL encoded.
     *
     * @param header  The JSON Header as a byte array.
     * @param payload The JSON Payload as a byte array
     * @param secret  The secret as a byte array
     */
    public byte[] signData(byte[] header, byte[] payload, byte[] secret) {
        try {
            // Look for a secret first and foremost.  Without it, we can't
            // sign anything.
            if ((secret == null) || (secret.length == 0)) {
                // No secret.  We can't sign this!
                return null;
            }

            Mac mac;
            mac = Mac.getInstance(javaAlgorithm);
            mac.init(new SecretKeySpec(secret, javaAlgorithm));

            byte[] jwsSecuredInput = createSecuredInput(header, payload);

            byte[] signature;
            signature = mac.doFinal(jwsSecuredInput);
            return signature;
        } catch (Exception e) {
            return null;    // No algorithm, no signing.
        }
    }

    /**
     * Validates the passed token using the passed secret.
     * All parameters must be base64URL Encoded
     *
     * @param header    The JSON Header as a byte array.
     * @param payload   The JSON Payload as a byte array
     * @param signature The signature as a byte array
     * @param secret    The secret as a byte array
     */
    public boolean validateToken(byte[] header, byte[] payload, byte[] signature, byte[] secret) {
        // All three params are required.
        if ((header == null) || (payload == null) || (signature == null) || (secret == null)) {
            logComparisonFailureError("Token Validation Failed: Parts of the token are missing", header, payload, signature, null);
            return false;   // Can't continue.  pieces missing.
        }

        if ((header.length == 0) || (payload.length == 0) || (signature.length == 0) || (secret.length == 0)) {
            logComparisonFailureError("Token Validation Failed: Parts of the token are empty", header, payload, signature, null);
            return false;   // Can't continue.  pieces empty.
        }


        byte[] sigToCompare = this.signData(header, payload, secret);
        byte[] decodedSignature = decode(signature);

        if (sigToCompare.length != decodedSignature.length) {
            logComparisonFailureError("Token Validation Failed: The signature length is invalid for this algorithm", header, payload, signature, sigToCompare);
            return false;   // The sigs already don't match.
        }

        // Compare the signatures byte for byte.
        for (int curIndex = 0; curIndex < decodedSignature.length; curIndex++) {
            if (decodedSignature[curIndex] != sigToCompare[curIndex]) {
                // Signatures don't match
                logComparisonFailureError("Token Validation Failed: The signatures do not match", header, payload, signature, sigToCompare);
                return false;
            }
        }

        return true;
    }

    private void logComparisonFailureError(String error, byte[] header, byte[] payload, byte[] signature, byte[] comparedSignature) {
        logger.log(Level.SEVERE, error + "\n  Received Token Header: [" + (header == null ? " (No header)" : new String(header)) + "]" +
                "\n                Payload: [" + (payload == null ? "(No payload)" : new String(payload)) + "]" +
                "\n              Signature: [" + (signature == null ? "(No signature)" : new String(signature)) + "]" +
                "\n    Generated Signature: [" + (comparedSignature == null ? "(No comparison signature)" : new String(encode(comparedSignature))) + "]");
    }

}
