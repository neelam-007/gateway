package com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.JsonWebSignature;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.JwsNone;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.hmac.JwsHmac;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.rsa.JwsRsa;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MissingJwtClaimsException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MultipleJwtClaimsException;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import com.l7tech.security.cert.TrustedCertManager;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: rseminoff
 * Date: 30/11/12
 */
public class JsonWebToken {

    private JsonWebSignature signatureAlgorithm = null;
    private JwtHeader jwtHeader = null;
    private String payload = null;
    private byte[] secret = null;

    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    private TrustedCertManager tcm = null;

    private static final Logger logger = Logger.getLogger(JsonWebToken.class.getName());

    public static JsonWebToken initTokenWithJwtHeader(@NotNull String customHeader, @NotNull String jsonPayload, @Nullable String secret, boolean secretIsBase64URLEncoded) throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IllegalJwtSignatureException, IOException {

        // secret should be NULL if none signature is used.
        JsonWebToken jwt = new JsonWebToken();
        jwt.replaceHeader(customHeader); // Sets signature algorithm automatically.
        jwt.setPayload(jsonPayload);
        jwt.setSecret(secret, secretIsBase64URLEncoded);

        return jwt;
    }

    public static JsonWebToken initSignedTokenWithAppendedClaims(@NotNull String appendClaims, @NotNull String jsonPayload, @NotNull String signatureAlgorithm, @NotNull String secret, boolean secretIsBase64URLEncoded) throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IllegalJwtSignatureException, IOException {

        // Signature Algorithm and secret cannot be null.
        JsonWebToken jwt = new JsonWebToken();
        jwt.appendHeader(appendClaims);
        jwt.setPayload(jsonPayload);
        jwt.setSignatureAlgorithmByName(signatureAlgorithm);
        jwt.setSecret(secret, secretIsBase64URLEncoded);

        return jwt;
    }

    public static JsonWebToken initNoneSignedTokenWithAppendedClaims(@NotNull String appendClaims, @NotNull String jsonPayload) throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IllegalJwtSignatureException, IOException {

        // THIS USES NONE SIGNATURE ALGORITHM.
        JsonWebToken jwt = new JsonWebToken();
        jwt.appendHeader(appendClaims);
        jwt.setPayload(jsonPayload);
        jwt.setSignatureAlgorithmByName("none");

        return jwt;
    }

    public static JsonWebToken initSimpleSignedToken(@NotNull String jsonPayload, @NotNull String signatureAlgorithm, @NotNull String secret, boolean secretIsBase64URLEncoded) throws InvalidJsonException, IllegalJwtSignatureException, IllegalArgumentException {

        // SECRET CANNOT BE NULL.
        // Signature Algorithm and secret cannot be null.
        JsonWebToken jwt = new JsonWebToken();
        jwt.setPayload(jsonPayload);
        jwt.setSignatureAlgorithmByName(signatureAlgorithm);
        jwt.setSecret(secret, secretIsBase64URLEncoded);

        return jwt;
    }

    public static JsonWebToken initSimpleNoneSignedToken(@NotNull String jsonPayload) throws InvalidJsonException, IllegalJwtSignatureException {
        // THIS ALSO USES NONE ALGORITHM
        JsonWebToken jwt = new JsonWebToken();
        jwt.setPayload(jsonPayload);
        jwt.setSignatureAlgorithmByName("none");

        return jwt;
    }

    public static JsonWebToken initEmptyToken() {
        return new JsonWebToken();
    }

    // Private constructor - use static methods to create this token now.
    private JsonWebToken() { }

    @Deprecated
    public JsonWebToken(String algorithm, String payload) throws InvalidJsonException, IllegalJwtSignatureException {
        this.setSignatureAlgorithmByName(algorithm);
        this.setPayload(payload);
    }

    @Deprecated
    public JsonWebToken(String algorithm, String payload, String secret, boolean secretIsBase64Encoded) throws InvalidJsonException, IllegalJwtSignatureException {
        this.setSignatureAlgorithmByName(algorithm);
        this.setPayload(payload);
        if (secretIsBase64Encoded) this.setSecretAsBase64UrlEncodedSecret(secret);
        else this.setSecret(secret);
    }

    public void replaceHeader(String newJwtHeader) throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IllegalJwtSignatureException, IOException {

        // FIXES MAG-68
        if  (!newJwtHeader.startsWith("{")) {
            // Missing an opening brace for a full header
            throw new InvalidJsonException("The replacement header is missing the opening brace '{'");
        }

        if (!newJwtHeader.endsWith("}")) {
            // Missing a closing brace for a full header
            throw new InvalidJsonException("The replacement header is missing the closing brace '}'");
        }
        // END - FIXES MAG-68

        if (jwtHeader == null) {
            jwtHeader = new JwtHeader();
        }
        jwtHeader.setCustomHeader(newJwtHeader);
        // The new header contains the signature algorithm to use.
        // This token object knows what that is after setting the header.
        this.setSignatureAlgorithmByName(jwtHeader.getSignatureAlgorithm());
    }

    public void appendHeader(String appendedJwtClaims) throws InvalidJsonException, MultipleJwtClaimsException, MissingJwtClaimsException, IOException {
        if (jwtHeader == null) {
            jwtHeader = new JwtHeader();
        }
        jwtHeader.appendToHeader(appendedJwtClaims);
    }

    @Deprecated
    public void setPayload(byte[] payload) throws InvalidJsonException, IllegalArgumentException {
        if ((payload == null) || (payload.length == 0)) {
            throw new IllegalArgumentException("The passed payload was null or zero length");
        }

        this.setPayload(new String(payload));
    }

    public void setPayload(String payload) throws IllegalArgumentException, InvalidJsonException {
        if ((payload == null) || (payload.trim().isEmpty())) {
            throw new IllegalArgumentException("The passed payload was null or zero length");
        }

        this.validateJsonPayload(payload);
        this.payload = payload;
    }

    // Deprecated - use getPayload() instead
    @Deprecated
    public String getPayloadAsString() {
        return this.getPayload();
    }

    public String getPayload() {
        if (payload.trim().isEmpty()) return "";    // Return an empty payload if there is none.
        else return payload;
    }

    // This has been deprecated - there is no guarantee the payload is JSON data.
    @Deprecated
    public JSONData getPayloadAsJsonData() {
        if (payload != null) {
            return JSONFactory.getInstance().newJsonData(payload);
        } else return null;
    }

    public void setSignatureAlgorithmByName(String algorithm) throws IllegalJwtSignatureException {
        signatureAlgorithm = JsonWebSignature.getAlgorithm(algorithm);

        if (jwtHeader == null) {
            jwtHeader = new JwtHeader();
        }

        jwtHeader.setSignatureAlgorithm(signatureAlgorithm.getAlgorithmName());
    }

    public String getSignatureAlgorithmName() {
        if (signatureAlgorithm != null) {
            return signatureAlgorithm.getAlgorithmName();
        }
        return null;
    }

    public void setSecret(byte[] passedSecret, boolean secretIsBase64Encoded) throws IllegalArgumentException {
        if (secretIsBase64Encoded) {
            // The secret is explicitly set to be base64 (url) encoded.
            // It is DECODED before storing in the secret byte array.
            if (passedSecret == null) {
                passedSecret = new byte[0]; // Create an empty secret.
            }

            if (passedSecret.length >= 4) {
                this.secret = JwtUtilities.decode(passedSecret);
            } else {
                throw new IllegalArgumentException("The Base64 URL Encoded Secret must be at least 4 characters long");
            }
            return;
        }

        this.secret = new byte[passedSecret.length];
        System.arraycopy(passedSecret, 0, this.secret, 0, passedSecret.length);

    }

    public void setSecret(String passedSecret, boolean secretIsBase64Encoded) throws IllegalArgumentException {
        if (passedSecret == null) {
            passedSecret = "";
        }
        setSecret(passedSecret.getBytes(), secretIsBase64Encoded);
    }

    @Deprecated
    public void setSecret(String passedSecret) {
        if (passedSecret == null) {
            passedSecret = "";
        }
        setSecret(passedSecret.getBytes());
    }

    @Deprecated
    public void setSecret(byte[] passedSecret) {
        this.secret = new byte[passedSecret.length];
        System.arraycopy(passedSecret, 0, this.secret, 0, passedSecret.length);
    }

    @Deprecated
    public void setSecretAsBase64UrlEncodedSecret(byte[] passedSecret) {
        // The secret is explicitly set to be base64 (url) encoded.
        // It is DECODED before storing in the secret byte array.
        if (passedSecret == null) {
            passedSecret = new byte[0]; // Create an empty secret.
        }

        if (passedSecret.length >= 4) {
            this.secret = JwtUtilities.decode(passedSecret);
        } else {
            throw new IllegalArgumentException("The Base64 URL Encoded Secret must be at least 4 characters long");
        }
    }

    @Deprecated
    public void setSecretAsBase64UrlEncodedSecret(String passedSecret) {
        if (passedSecret == null) {
            setSecretAsBase64UrlEncodedSecret(new byte[0]);
            return;
        }
        setSecretAsBase64UrlEncodedSecret(passedSecret.getBytes());
    }

    public void setSecretAsPrivateKey(PrivateKey privateKey) {
        if (privateKey != null) {
            this.privateKey = privateKey;
        } else {
            throw new IllegalArgumentException("Private key cannot be null");
        }
    }

    public void setSecretAsPublicKey(PublicKey publicKey) {
        if (publicKey != null) {
            this.publicKey = publicKey;
        } else {
            throw new IllegalArgumentException("Public key cannot be null");
        }
    }

    public byte[] getToken() {
        // We actually perform the token signing and generation at this point.
        // Make sure all parts are in place first.

        byte[] tokenHeader = this.getHeader().getBytes();

        if ((tokenHeader == null) || (signatureAlgorithm == null) || (payload == null)) {
            // We can't continue.
            throw new IllegalArgumentException("The header and/or signature algorithm and/or the payload are not set");
        }

        byte[] encodedHeader = JwtUtilities.encode(tokenHeader);
        byte[] encodedPayload = JwtUtilities.encode(payload.getBytes());

        if ((encodedHeader == null) || (encodedPayload == null)) {
            return null;    // Can't continue.
        }

        byte[] signature;

        if (signatureAlgorithm instanceof JwsRsa) {
            // Use the public/private key secrets.
            JwsRsa castSigAlg = (JwsRsa) signatureAlgorithm;
            signature = castSigAlg.signData(encodedHeader, encodedPayload, privateKey);
        } else if (signatureAlgorithm instanceof JwsHmac) {
            JwsHmac castSigAlg = (JwsHmac) signatureAlgorithm;
            signature = castSigAlg.signData(encodedHeader, encodedPayload, secret);
//      FOR FUTURE ELLIPTICAL CURVE SIGNING
//        } else if (signatureAlgorithm instanceof JwsEcdsa) {
//            JwsEcdsa castSigAlg = (JwsEcdsa) signatureAlgorithm;
//            signature = castSigAlg.signData(encodedHeader, encodedPayload, [ellipticalData]);
        } else { // signatureAlgorithm instanceof JwsNone
            JwsNone castSigAlg = (JwsNone) signatureAlgorithm;
            signature = castSigAlg.signData(encodedHeader, encodedPayload);
        }

        if (signature != null) {
            return join(encodedHeader, encodedPayload, ((signature.length > 0) ? JwtUtilities.encode(signature) : new byte[0]));
        } else {
            return null;    // No signature means there was a signing issue, we return null to the caller as this is a failure.
        }
    }

    public String getTokenAsString() {
        String stringToken = new String(this.getToken());
        if (stringToken.trim().isEmpty()) return null;
        else return stringToken;
    }

    public String getHeader() {
        return jwtHeader.getHeader();
    }

    @Deprecated
    public String getHeaderAsString() {
        String header = this.getHeader();
        if (header.trim().isEmpty()) return null;
        else return header;
    }

    public JSONData getHeaderAsJsonData() {
        String header = this.getHeader();
        if (header != null) {
            return JSONFactory.getInstance().newJsonData(header);
        } else {
            return null;
        }
    }

    /**
     * Validates a JSON Web Token.  The secret may be null if it is not needed to validate the token.
     *
     * @param token                 The Token to validate.  Must not be null or empty
     * @param secret                A byte array containing the secret (like a password).  May be null if not needed.
     * @param secretIsBase64Encoded Pass true if the secret is ALREADY base 64 or base 64 URL encoded and needs to be decoded.
     * @return true if the token is valid and passes all validation checks.  False otherwise.
     */
    public boolean validateReceivedToken(@NotNull @NotEmpty byte[] token, @NotEmpty byte[] secret, boolean secretIsBase64Encoded) throws IllegalJwtSignatureException {
        if (secretIsBase64Encoded) this.setSecretAsBase64UrlEncodedSecret(secret);
        else this.setSecret(secret);
        return this.validateByteToken(checkToken(token));
    }

    /**
     * Validates a JSON Web Token that requires a public key.
     *
     * @param token     The byte array token to validate.  Must not be null or empty
     * @param publicKey The public key used to validate the token.  Must not be null.
     * @return true if the token is valid and passes all validation checks.  False otherwise.
     */
    public boolean validateReceivedToken(@NotNull @NotEmpty byte[] token, @NotNull PublicKey publicKey) throws IllegalJwtSignatureException {
        this.setSecretAsPublicKey(publicKey);
        return this.validateByteToken(checkToken(token));
    }

    /**
     * Validates a JSON Web token in a String using a byte array secret
     *
     * @param token                 A String containing the token.  Must not be null or empty
     * @param secret                A byte array containing the secret.  May be null if no secret is required.
     * @param secretIsBase64Encoded Pass true if the secret is ALREADY base 64 or base 64 URL encoded and neeeds to be decoded
     * @return true if the token is valid and passes all validation checks.  False otherwise.
     */
    public boolean validateReceivedToken(@NotNull @NotEmpty String token, @NotEmpty byte[] secret, boolean secretIsBase64Encoded) throws IllegalJwtSignatureException {
        if (secretIsBase64Encoded) this.setSecretAsBase64UrlEncodedSecret(secret);
        else this.setSecret(secret);
        return this.validateByteToken(checkToken(token));
    }

    /**
     * Validates a JSON Web Token in a String using a Public Key.
     *
     * @param token     A String containing the token.  Must not be null or empty
     * @param publicKey The Public Key used to validate the token.  Must not be null.
     * @return true if the token is valid and passes all validation checks.  False otherwise.
     */
    public boolean validateReceivedToken(@NotNull @NotEmpty String token, @NotNull PublicKey publicKey) throws IllegalJwtSignatureException {
        this.setSecretAsPublicKey(publicKey);
        return this.validateByteToken(checkToken(token));
    }

    public boolean validateReceivedTokenNoSecret(@NotNull @NotEmpty byte[] token) throws IllegalJwtSignatureException {
        this.secret = null;
        return this.validateByteToken(checkToken(token));
    }

    public boolean validateReceivedTokenNoSecret(@NotNull @NotEmpty String token) throws IllegalJwtSignatureException {
        return this.validateByteToken(checkToken(token));
    }

    public boolean validateReceivedTokenNoSecret(@NotNull @NotEmpty String token, @NotNull TrustedCertManager tcm) throws IllegalJwtSignatureException {
        this.tcm = tcm;
        return this.validateByteToken(checkToken(token));
    }

    /**
     * *** PRIVATE METHODS ******
     */
    private byte[] checkToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Cannot validate a null token");
        }

        if (token.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot validate an empty token");
        }
        return token.getBytes();
    }

    private byte[] checkToken(byte[] token) {
        return this.checkToken(new String(token));
    }

    // This was made Private as it doesn't explicitly declare that it requires a secret to
    // validate a token.  There are public methods that take a token and secret to perform the validation.
    private boolean validateByteToken(byte[] token) throws IllegalJwtSignatureException {
        // This is taken from the decode assertion.
        if ((token == null) || (token.length < 4)) {
            return false;
        }

        // We need to determine how many parts there are to this token first.
        // If it's two, we don't need a secret, if it's three, then we do.
        byte[][] splitToken = split(token);

        if (splitToken == null) {
            logTokenError("Received Invalid Token:  Unknown format.", token);
            return false;
        }

        if ((splitToken.length < 2) || (splitToken.length > 3)) {
            // There should be at least two parts to the token, but not more than 3.
            // This fails.
            logTokenError("Received Invalid Token: JSON Header is less than 4 characters.", token);
            return false;
        }

        byte[] header = splitToken[0];
        byte[] payload = splitToken[1];
        byte[] signature = null;
        if (splitToken.length == 3) {
            signature = splitToken[2];
        }

        // Do we even have a header to validate?
        if (header.length < 4) {
            // The header is invalid.  It should be at least 4 characters.
            // (as it's base64 encoded at the moment)
            logTokenError("Received Invalid Token: JSON Header is less than 4 characters.", token);
            return false;
        }

        if (payload.length < 4) {
            // The payload is invalid for the same reasons as the header above.
            logTokenError("Received Invalid Token: JSON Payload is less than 4 characters.", token);
            return false;
        }

        // We need access to the header to determine the algorithm
        // Decode it first
        byte[] decodedHeader = JwtUtilities.decode(header);
        if (decodedHeader == null) {
            // Something bad happened on the decode.  Fail.
            logTokenError("Received Invalid Token: The JSON Header could not be decoded.", token);
            return false;
        }

        try {
            jwtHeader = new JwtHeader();
            jwtHeader.setCustomHeader(new String(decodedHeader));
        } catch (InvalidJsonException | IOException e) {
            logTokenError("Received Invalid Token: The JSON Header is not valid JSON", token);
            return false;   // bad JSON.
        } catch (MultipleJwtClaimsException e) {
            logTokenError("Received Invalid Token: The JSON Header contains duplicate claims and cannot be used.", token);
            return false;   // bad JSON.
        } catch (MissingJwtClaimsException e) {
            logTokenError("Received Invalid Token: The JSON Header doesn't contain the signature algorithm used.", token);
            return false;   // bad JSON.
        }

        // We have our algorithm.  Let's get the JWS object that implements it.
        JsonWebSignature jws = JsonWebSignature.getAlgorithm(jwtHeader.getSignatureAlgorithm());

        if (jws == null) {
            // Too bad...we don't support the algorithm requested.  Fail.
            logTokenItemInfo("Received Invalid Token: The signature algorithm (" + jwtHeader.getSignatureAlgorithm() + ") is not supported by the SSG", header, payload, signature);
            return false;
        }

        // Validate the token.
        if (jws instanceof JwsHmac) {
            JwsHmac castJws = (JwsHmac) jws;
            if (!castJws.validateToken(header, payload, signature, secret)) {
                // If the validation fails, it will be already logged.
                return false;
            }
        } else if (jws instanceof JwsRsa) {
            JwsRsa castJws = (JwsRsa) jws;

            // RIGHT HERE:
            // If there is an x5t claim in the header, use IT and ignore any existing secret (Including NONE).
            // (This applies only to RSA signed tokens)
            // Apply a second method to RSA that accepts a thumbprint, and it can retrieve it from the gateway.
            // Break the header into a map, then grab x5t out of the map if it exists and use it.
            String x5tData = jwtHeader.getName("x5t");
            if (x5tData != null) {
                // We have a thumbprint...try to use that to get the public key.
                if (!castJws.validateToken(header, payload, signature, x5tData, tcm)) {
                    return false;
                }
            } else {
                if (!castJws.validateToken(header, payload, signature, publicKey)) {
                    // If the validation fails, it will be already logged.
                    return false;
                }
            }


//      FOR FUTURE ELLIPTICAL CURVE VALIDATION
//        } else if (jws instanceof JwsEcdsa) {
//            JwsEcdsa castJws = (JwsEcdsa) jws;
//            if (!castJws.validateToken(header, payload, signature, [ellipticalData])) {
            // If the validation fails, it will be already logged.
//                return false;
//            }
        } else {
            JwsNone castJws = (JwsNone) jws;
            if (!castJws.validateToken(header, payload, signature)) {
                // If the validation fails, it will be already logged.
                return false;
            }
        }

        // Save decoded payload.
        this.payload = new String(JwtUtilities.decode(payload));
        return true;
    }

    private byte[] join(byte[] header, byte[] payload, byte[] signature) {
        byte separator[] = ".".getBytes();
        byte token[] = new byte[header.length + separator.length + payload.length + separator.length + signature.length];

        System.arraycopy(header, 0, token, 0, header.length);
        System.arraycopy(separator, 0, token, header.length, separator.length);
        System.arraycopy(payload, 0, token, header.length + separator.length, payload.length);
        System.arraycopy(separator, 0, token, header.length + separator.length + payload.length, separator.length);
        System.arraycopy(signature, 0, token, header.length + separator.length + payload.length + separator.length, signature.length);

        return token;
    }

    /**
     * Splits a token into it's component parts.
     * byte[0][] is the header
     * byte[1][] is the payload
     * byte[2][] is the signature, if available.
     */
    private byte[][] split(byte[] token) {
        // We can "cheat".  We know the token is base64 URL encoded.
        // Instead of going through a crapload of bytes, we can convert the token into a string
        // and use a regex split on it.
        String tokenString = new String(token);
        String[] tokenParts = tokenString.split("[.]");

        if ((tokenParts.length < 2) || (tokenParts.length > 3)) {
            // The token is invalid, there's less than two parts or more than three parts of it.
            return null;
        }

        // We use .getBytes().length on the strings to handle any UTF8 chars that are more than a single byte
        // representation, otherwise we overflow or truncate.
        byte[][] splitBytes = new byte[tokenParts.length][];
        splitBytes[0] = new byte[tokenParts[0].getBytes().length];
        splitBytes[1] = new byte[tokenParts[1].getBytes().length];
        System.arraycopy(tokenParts[0].getBytes(), 0, splitBytes[0], 0, tokenParts[0].getBytes().length);
        System.arraycopy(tokenParts[1].getBytes(), 0, splitBytes[1], 0, tokenParts[1].getBytes().length);

        if (splitBytes.length == 3) {
            splitBytes[2] = new byte[tokenParts[2].getBytes().length];
            System.arraycopy(tokenParts[2].getBytes(), 0, splitBytes[2], 0, tokenParts[2].getBytes().length);
        }

        return splitBytes;
    }

    public void validateJsonPayload(String payload) throws InvalidJsonException {

        // Invalid JSON won't make it through this.
        JSONData jsonHeader = JSONFactory.getInstance().newJsonData(payload);
        // Mitigates MAG-65, also allows booleans to be used in the payload.
        try {
            Map<String, Object> objectMap = (Map<String, Object>) jsonHeader.getJsonObject();
        } catch (InvalidJsonException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidJsonException("The payload could not be parsed.");
        }
        // We don't check for multiple claims in the payload, as it's not our
        // responsibility to do so.  JWT specifies uniqueness in the header, not the payload.
    }

    private void logTokenItemError(@NotNull @NotEmpty String error, byte[] header, byte[] payload, byte[] signature) {
        logTokenItems(Level.SEVERE, error, header, payload, signature);
    }

    private void logTokenItemInfo(@NotNull @NotEmpty String error, byte[] header, byte[] payload, byte[] signature) {
        logTokenItems(Level.INFO, error, header, payload, signature);
    }

    private void logTokenItems(Level logLevel, String error, byte[] header, byte[] payload, byte[] signature) {
        logger.log(logLevel, error + "\n  Received Token Header: [" + (header == null ? " (No header)" : new String(header)) + "]" +
                "\n                Payload: [" + (payload == null ? "(No payload)" : new String(payload)) + "]" +
                "\n              Signature: [" + (signature == null ? "(No signature)" : new String(signature)) + "]");
    }

    private void logTokenError(String error, byte[] token) {
        logger.log(Level.SEVERE, error + "\n  Received Token (Header.Payload.Signature): [" + (token == null ? "(no token)" : new String(token)) + "]");
    }
}
