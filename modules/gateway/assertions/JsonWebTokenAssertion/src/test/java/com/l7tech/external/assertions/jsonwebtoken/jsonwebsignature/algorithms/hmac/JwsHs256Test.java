package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.hmac;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JsonWebToken;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.json.InvalidJsonException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * User: rseminoff
 * Date: 03/12/12
 */
public class JwsHs256Test {

    private JsonWebToken jwt;

    private byte[] secret;
    private byte[] token;

    @Before
    public void setup() throws IllegalJwtSignatureException {
        jwt = JsonWebToken.initEmptyToken();   // Used for validation only.
        jwt.setSignatureAlgorithmByName(JwsHs256.jwsAlgorithmName);
        jwt.setSecret((String)null, false);

        token = null;
    }

    // This test will perform a normal signing with a header and payload.
    // Then the signed data is validated.  It should pass as it has all
    // proper values.
    // This uses a Base64URL Encoded Secret
    @Test
    public void testSigning_1() throws IllegalJwtSignatureException {

        // The token creation shouldn't fail, as all parts are set.
        try {
            jwt.setPayload("{\"testPayload\":\"aS91Dkf912_9123asdjcasfFL(283LADwe94j)\"}");
            secret = "SecretPassword".getBytes();
            jwt.setSecret(JwtUtilities.encode(secret), true);
            token = jwt.getToken();
            assertTrue("The token just created and signed has failed to validate. (Proper Token)", jwt.validateReceivedToken(token, JwtUtilities.encode(secret), true));
        } catch (InvalidJsonException e) {
            fail("The JSON payload is invalid.  This is a failure, as the JSON is valid.");
        }
    }

    // This test will perform a normal signing with a header and payload.
    // Then the signed data is validated.  It should pass as it has all
    // proper values.
    // This uses a Plaintext Secret
    @Test
    public void testSigning_1_1() throws IllegalJwtSignatureException {

        // The token creation shouldn't fail, as all parts are set.
        try {
            jwt.setPayload("{\"testPayload\":\"aS91Dkf912_9123asdjcasfFL(283LADwe94j)\"}");
            secret = "SecretPassword".getBytes();
            jwt.setSecret(secret, false);
            token = jwt.getToken();
            assertTrue("The token just created and signed has failed to validate. (Proper Token)", jwt.validateReceivedToken(token, secret, false));
        } catch (InvalidJsonException e) {
            fail("The JSON payload is invalid. This is a failure, as the JSON is valid.");
        }
    }

    // This test will perform a normal signing with a header and payload.
    // Then the signed data is validated.  It should pass as it has all
    // proper values.
    // This uses a Plaintext Secret to sign the token, a Base64URL Encoded Secret to validate it.
    @Test
    public void testSigning_1_2() throws IllegalJwtSignatureException {

        // The token creation shouldn't fail, as all parts are set.
        try {
            jwt.setPayload("{\"testPayload\":\"aS91Dkf912_9123asdjcasfFL(283LADwe94j)\"}");
            secret = "SecretPassword".getBytes();
            jwt.setSecret(secret, false);
            token = jwt.getToken();
            assertTrue("The token just created and signed has failed to validate. (Proper Token)", jwt.validateReceivedToken(token, JwtUtilities.encode(secret), true));
        } catch (InvalidJsonException e) {
            fail("The JSON payload is invalid");
        }
    }

    // This test will perform a normal signing with a header and payload.
    // Then the signed data is validated.  It should pass as it has all
    // proper values.
    // This uses a Base64URL Secret to sign the token, a Plaintext Secret to validate it.
    @Test
    public void testSigning_1_3() throws IllegalJwtSignatureException {
        // The token creation shouldn't fail, as all parts are set.
        try {
            jwt.setPayload("{\"testPayload\":\"aS91Dkf912_9123asdjcasfFL(283LADwe94j)\"}");
            secret = "SecretPassword".getBytes();
            jwt.setSecret(JwtUtilities.encode(secret), true); // Base64URL Encoded Secret
            token = jwt.getToken();
            assertTrue("The token just created and signed has failed to validate. (Proper Token)", jwt.validateReceivedToken(token, secret, false));
        } catch (InvalidJsonException e) {
            fail("The JSON payload is invalid");
        }
    }

    // This test will perform a normal signing with a header but no payload
    @Test(expected = IllegalArgumentException.class)
    public void testSigning_2() throws InvalidJsonException {
        // This test "forgets" to pass a payload.  It should fail.
        // In this case, we pass a space as the JsonWebToken doesn't allow
        // Null or empty strings for payloads.
        jwt.setPayload(" ");
        fail("An empty JSON payload was allowed to be set.");
    }

    // This specifies a payload, but no signature algorithm.
    @Test(expected = IllegalJwtSignatureException.class)
    public void testSigning_3() throws InvalidJsonException, IllegalJwtSignatureException {
        // This test doesn't set a signature type.  It should fail.
        jwt.setPayload("{\"testPayload\":\"aS91Dkf912_9123asdjcasfFL(283LADwe94j)\"}");
        jwt.setSignatureAlgorithmByName("");
        fail("Attempting to explicitly set the Signature Algorithm to (empty string) didn't fail when it should.");
    }

    // This specifies a payload, and algorithm. but no secret.
    @Test
    public void testSigning_4() throws InvalidJsonException {
        // This test doesn't set a secret.  It should fail.
        jwt.setPayload("{\"testPayload\":\"aS91Dkf912_9123asdjcasfFL(283LADwe94j)\"}");
        token = jwt.getToken();
        assertNull("Able to create a token with HS256 with no secret. (Failure: There must be a secret for HMAC)", token);
    }

}