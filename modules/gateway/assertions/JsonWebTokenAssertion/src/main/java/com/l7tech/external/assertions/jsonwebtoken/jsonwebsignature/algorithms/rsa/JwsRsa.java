package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.rsa;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.JsonWebSignature;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;

import java.security.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.AVAILABLE_SECRET_KEY;
import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.decode;

/**
 * User: rseminoff
 * Date: 09/01/13
 */
public abstract class JwsRsa extends JsonWebSignature {

    private static final Logger logger = Logger.getLogger(JwsRsa.class.getName());

    public JwsRsa(String algorithmName, String description, String javaAlgorithm, Logger logger) {
        super(algorithmName, description, javaAlgorithm, logger, AVAILABLE_SECRET_KEY);
    }

    /**
     * This is called to sign digital data with the passed Private Key.
     * All parameters should be plaintext/plainbyte, and not Base64URL encoded.
     *
     * @param header  The JSON Header as a byte array.
     * @param payload The JSON Payload as a byte array
     * @param secret  The secret as a Private Key
     */
    public byte[] signData(byte[] header, byte[] payload, PrivateKey secret) {

        // The private Key is passed as the secret.
        // It needs to be built up first, as without it, there is no signing.
        Signature sig;
        try {
            sig = Signature.getInstance(javaAlgorithm);
        } catch (NoSuchAlgorithmException nsae) {
            logger.log(Level.SEVERE, "Missing signature algorithm: " + javaAlgorithm);
            return null;
        }

        if (secret == null) {
            logger.log(Level.SEVERE, "The Private Key required for signing is missing");
            return null;
        }

        try {
            sig.initSign(secret, new SecureRandom());
        } catch (InvalidKeyException ike) {
            logger.log(Level.SEVERE, "The selected private key is invalid");
            return null;
        }

        byte[] jwsSecuredInput = createSecuredInput(header, payload);

        try {
            sig.update(jwsSecuredInput);
        } catch (SignatureException se) {
            logger.log(Level.WARNING, "An error occurred during signature creation.  Check the private key settings.");
            return null;
        }

        // Attempt the signing.
        try {
            return sig.sign();
        } catch (SignatureException se) {
            logger.log(Level.SEVERE, "An error occurred during the signature creation.  Check the private key and variable settings.");
        }
        return null;    // Fail
    }

    /**
     * Validates the passed token using the passed Public Key.
     * All parameters should be Base64URLEncoded, except secret, which is a PublicKey
     *
     * @param header    The JSON Header as a byte array.
     * @param payload   The JSON Payload as a byte array
     * @param signature The signature as a byte array
     * @param secret    The secret as a Public Key
     */
    public boolean validateToken(byte[] header, byte[] payload, byte[] signature, PublicKey secret) {
        // The public key is passed as the secret.
        // It needs to be built up first, as without it, there is no validation
        Signature sig;
        try {
            sig = Signature.getInstance(javaAlgorithm);
        } catch (NoSuchAlgorithmException nsae) {
            logger.log(Level.SEVERE, "Missing signature algorithm: " + javaAlgorithm);
            return false;
        }

        if (secret == null) {
            logger.log(Level.WARNING, "The public key required for validation is missing. If in a context variable, the key is not in a format that can be used.");
            return false;
        }

        if ((signature == null) || (signature.length < 4)) {
            // The signature is invalid.
            logger.log(Level.WARNING, "The passed signature is invalid and cannot be used");
        }

        try {
            sig.initVerify(secret);
        } catch (InvalidKeyException ike) {
            logger.log(Level.SEVERE, "The public key of the selected private key is invalid");
            return false;
        }

        try {
            sig.update(createSecuredInput(header, payload));
        } catch (SignatureException se) {
            logger.log(Level.WARNING, "An error occurred during signature validation.  Check the private key settings.");
            return false;
        }

        // Attempt the signing.
        try {
            byte[] decodedSig = decode(signature);
            if (sig.verify(decodedSig)) {
                return true;    // The signature validated
            } else {
                logTokenError("Token Validation Failed: The signatures do not match", header, payload, signature);
            }
        } catch (Exception e) { // Signature Exception or a Base64 Decode exception
            logTokenError("Token Validation Failed: The token signature is invalid for this algorithm", header, payload, signature);
        }
        return false;    // Fail
    }

    public boolean validateToken(byte[] header, byte[] payload, byte[] signature, String certificateThumbprint, TrustedCertManager tcm) {
        // Get the public key certificate the thumbprint refers to.  The public key cert must be the cert of the
        // private key used to sign the token to start with.
        if (tcm == null) {
            logger.log(Level.SEVERE, "Unable to retrieve the certificate by thumbprint.");
            return false;
        }

        // MAG-216: While findByThumbprint only accept Base64 encoded SHA1 hashes, JWT uses Base64URL Encoded SHA1 hashes.
        // This will work around that limitation by decoding the incoming thumbprint regardless of encoding and encode it
        // to Base64 for findByThumbprint.
        String convertedThumbprint;
        try {
            byte[] incomingThumbprint = decode(certificateThumbprint.getBytes());
            if (incomingThumbprint.length > 0) {
                // Convert the decoded thumbprint to base64 for certificate location
                convertedThumbprint = new String(org.apache.commons.codec.binary.Base64.encodeBase64(incomingThumbprint));
            } else {
                logger.log(Level.WARNING, "Incoming certificate thumbprint is invalid");
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to process incoming certificate thumbprint");
            return false;
        }

        try {
            List<TrustedCert> certList = tcm.findByThumbprint(convertedThumbprint);
            // If we can't get the certificate, fail.
            if (certList.isEmpty()) {
                logger.log(Level.WARNING, "Unable to locate certificate specified by thumbprint");
                return false;
            }

            // Once we have the certificate, call the other version of this method, passing the public key.
            return this.validateToken(header, payload, signature, certList.get(0).getCertificate().getPublicKey());

        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to locate certificate specified by thumbprint");
            return false;
        }
    }

    private void logTokenError(String error, byte[] header, byte[] payload, byte[] signature) {
        logger.log(Level.SEVERE, error + "\nReceived Token Header: [" + (header == null ? " (No header)" : new String(header)) + "]" +
                "\n              Payload: [" + (payload == null ? "(No payload)" : new String(payload)) + "]" +
                "\n            Signature: [" + (signature == null ? "(No signature)" : new String(signature)) + "]");
    }
}
