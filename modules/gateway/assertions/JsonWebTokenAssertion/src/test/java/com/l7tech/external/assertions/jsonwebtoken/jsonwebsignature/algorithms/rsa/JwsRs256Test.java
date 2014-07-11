package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.rsa;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JsonWebToken;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MissingJwtClaimsException;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.MultipleJwtClaimsException;
import com.l7tech.json.InvalidJsonException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import static org.junit.Assert.*;

/**
 * User: rseminoff
 * Date: 12/12/12
 */

public class JwsRs256Test {
    private JsonWebToken jwt;

    // No secret here to share for signing test.
    private byte[] token;

    @Before
    public void setup() throws IllegalJwtSignatureException {
        jwt = JsonWebToken.initEmptyToken();   // Used for validation only.
        jwt.setSignatureAlgorithmByName(JwsRs256.jwsAlgorithmName);
        token = null;
    }

    @Test
    public void testCreateRSASigning_Baseline() throws IllegalJwtSignatureException, MultipleJwtClaimsException, MissingJwtClaimsException {
        // This tests a standard RSA signature signing and validation.
        // This test uses the example from the json-web-signature-07 document, appendix A.2: JWS using RSA SHA-256
        // It MANUALLY creates the private key/public key/exponent for testing and validation.
        // This allows testing against known values.
        // The token creation shouldn't fail, as all parts are set.
        byte[] modulus = {
                (byte) 161, (byte) 248, 22, 10, (byte) 226, (byte) 227, (byte) 201, (byte) 180, 101, (byte) 206, (byte) 141, 45, 101, 98, 99, 54, 43, (byte) 146, 125,
                (byte) 190, 41, (byte) 225, (byte) 240, 36, 119, (byte) 252, 22, 37, (byte) 204, (byte) 144, (byte) 161, 54, (byte) 227, (byte) 139, (byte) 217, 52, (byte) 151, (byte) 197,
                (byte) 182, (byte) 234, 99, (byte) 221, 119, 17, (byte) 230, 124, 116, 41, (byte) 249, 86, (byte) 176, (byte) 251, (byte) 138, (byte) 143, 8, (byte) 154, (byte) 220,
                75, 105, (byte) 137, 60, (byte) 193, 51, 63, 83, (byte) 237, (byte) 208, 25, (byte) 184, 119, (byte) 132, 37, 47, (byte) 236, (byte) 145, 79,
                (byte) 228, (byte) 133, 119, 105, 89, 75, (byte) 234, 66, (byte) 128, (byte) 211, 44, 15, 85, (byte) 191, 98, (byte) 148, 79, 19, 3, (byte) 150,
                (byte) 188, 110, (byte) 155, (byte) 223, 110, (byte) 189, (byte) 210, (byte) 189, (byte) 163, 103, (byte) 142, (byte) 236, (byte) 160, (byte) 198, 104, (byte) 247, 1, (byte) 179,
                (byte) 141, (byte) 191, (byte) 251, 56, (byte) 200, 52, 44, (byte) 226, (byte) 254, 109, 39, (byte) 250, (byte) 222, 74, 90, 72, 116, (byte) 151, (byte) 157,
                (byte) 212, (byte) 185, (byte) 207, (byte) 154, (byte) 222, (byte) 196, (byte) 199, 91, 5, (byte) 133, 44, 44, 15, 94, (byte) 248, (byte) 165, (byte) 193, 117, 3,
                (byte) 146, (byte) 249, 68, (byte) 232, (byte) 237, 100, (byte) 193, 16, (byte) 198, (byte) 182, 71, 96, (byte) 154, (byte) 164, 120, 58, (byte) 235, (byte) 156,
                108, (byte) 154, (byte) 215, 85, 49, 48, 80, 99, (byte) 139, (byte) 131, 102, 92, 111, 111, 122, (byte) 130, (byte) 163, (byte) 150, 112,
                42, 31, 100, 27, (byte) 130, (byte) 211, (byte) 235, (byte) 242, 57, 34, 25, 73, 31, (byte) 182, (byte) 134, (byte) 135, 44, 87, 22, (byte) 245,
                10, (byte) 248, 53, (byte) 141, (byte) 154, (byte) 139, (byte) 157, 23, (byte) 195, 64, 114, (byte) 143, 127, (byte) 135, (byte) 216, (byte) 154, 24, (byte) 216,
                (byte) 252, (byte) 171, 103, (byte) 173, (byte) 132, 89, 12, 46, (byte) 207, 117, (byte) 147, 57, 54, 60, 7, 3, 77, 111, 96, 111,
                (byte) 158, 33, (byte) 224, 84, 86, (byte) 202, (byte) 229, (byte) 233, (byte) 161
        };
        byte[] exponent = {1, 0, 1};
        byte[] privateExponent = {
                18, (byte) 174, 113, (byte) 164, 105, (byte) 205, 10, 43, (byte) 195, 126, 82, 108, 69, 0, 87, 31, 29, 97, 117, 29,
                100, (byte) 233, 73, 112, 123, 98, 89, 15, (byte) 157, 11, (byte) 165, 124, (byte) 150, 60, 64, 30, 63, (byte) 207, 47, 44,
                (byte) 211, (byte) 189, (byte) 236, (byte) 136, (byte) 229, 3, (byte) 191, (byte) 198, 67, (byte) 155, 11, 40, (byte) 200, 47, 125, 55, (byte) 151, 103, 31,
                82, 19, (byte) 238, (byte) 216, (byte) 193, 90, 37, (byte) 216, (byte) 213, (byte) 206, (byte) 160, 2, 94, (byte) 227, (byte) 171, 46, (byte) 139, 127, 121,
                33, 111, (byte) 198, 59, (byte) 234, 86, 39, 83, (byte) 180, 6, 68, (byte) 198, (byte) 161, 81, 39, (byte) 217, (byte) 178, (byte) 149, 69, 64,
                (byte) 160, (byte) 187, (byte) 225, (byte) 163, 5, 86, (byte) 152, 45, 78, (byte) 159, (byte) 222, 95, 100, 37, (byte) 241, 77, 75, 113, 52, 65,
                (byte) 181, 93, (byte) 199, 59, (byte) 155, 74, (byte) 237, (byte) 204, (byte) 146, (byte) 172, (byte) 227, (byte) 146, 126, 55, (byte) 245, 125, 12, (byte) 253, 94,
                117, (byte) 129, (byte) 250, 81, 44, (byte) 143, 73, 97, (byte) 169, (byte) 235, 11, (byte) 128, (byte) 248, (byte) 168, 7, 70, 114, (byte) 138, 85,
                (byte) 255, 70, 71, 31, 52, 37, 6, 59, (byte) 157, 83, 100, 47, 94, (byte) 222, 30, (byte) 132, (byte) 214, 19, 8, 26, (byte) 250,
                92, 34, (byte) 208, 81, 40, 91, (byte) 214, 59, (byte) 148, 59, 86, 93, (byte) 137, (byte) 138, 5, 104, 84, 19, (byte) 229, 60, 60,
                108, 101, 37, (byte) 255, 31, (byte) 227, 78, 61, (byte) 220, 112, (byte) 240, (byte) 213, 100, 80, (byte) 253, (byte) 164, (byte) 139, (byte) 161, 46,
                16, 78, (byte) 157, (byte) 235, (byte) 159, (byte) 184, 24, (byte) 129, (byte) 225, (byte) 196, (byte) 189, (byte) 242, 93, (byte) 146, 71, (byte) 244, 80, (byte) 200, 101,
                (byte) 146, 121, 104, (byte) 231, 115, 52, (byte) 244, 65, 79, 117, (byte) 167, 80, (byte) 225, 57, 84, 110, 58, (byte) 138, 115,
                (byte) 157
        };

        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        try {
            RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(new BigInteger(1, modulus), new BigInteger(1, privateExponent));
            RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));

            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(publicSpec);
            privateKey = kf.generatePrivate(privateSpec);
        } catch (Exception e) {
            fail("Unable to create the RSA Private/Public Keys from supplied bytes.");
        }

        assertNotNull("Public Key is still null", publicKey);
        assertNotNull("Private Key is still null", privateKey);

        // Create the token.
        try {
            jwt.replaceHeader("{\"alg\":\"" + jwt.getSignatureAlgorithmName() + "\"}");
            jwt.setPayload("{\"iss\":\"joe\",\r\n" +
                    " \"exp\":1300819380,\r\n" +
                    " \"http://example.com/is_root\":true}");
            jwt.setSecretAsPrivateKey(privateKey);
            token = jwt.getToken();
        } catch (InvalidJsonException e) {
            fail("The payload is not valid JSON");
        } catch (IOException e) {
            fail("The payload is not valid JSON");
        }

        assertNotNull("No token", token);

        byte[] tokenArray = jwt.getToken();
        byte[] compareArray = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw".getBytes();

        assertArrayEquals("Token doesn't match RSA SHA-256 Appendix A.2 Test", tokenArray, compareArray);

    }

    @Test
    public void testValidateRSASigning_Baseline() throws IllegalJwtSignatureException, MultipleJwtClaimsException, MissingJwtClaimsException {
        // This tests a standard RSA signature signing and validation.
        // This test uses the example from the json-web-signature-07 document, appendix A.2: JWS using RSA SHA-256
        // It MANUALLY creates the private key/public key/exponent for testing and validation.
        // This allows testing against known values.
        // The token creation shouldn't fail, as all parts are set.
        byte[] modulus = {
                (byte) 161, (byte) 248, 22, 10, (byte) 226, (byte) 227, (byte) 201, (byte) 180, 101, (byte) 206, (byte) 141, 45, 101, 98, 99, 54, 43, (byte) 146, 125,
                (byte) 190, 41, (byte) 225, (byte) 240, 36, 119, (byte) 252, 22, 37, (byte) 204, (byte) 144, (byte) 161, 54, (byte) 227, (byte) 139, (byte) 217, 52, (byte) 151, (byte) 197,
                (byte) 182, (byte) 234, 99, (byte) 221, 119, 17, (byte) 230, 124, 116, 41, (byte) 249, 86, (byte) 176, (byte) 251, (byte) 138, (byte) 143, 8, (byte) 154, (byte) 220,
                75, 105, (byte) 137, 60, (byte) 193, 51, 63, 83, (byte) 237, (byte) 208, 25, (byte) 184, 119, (byte) 132, 37, 47, (byte) 236, (byte) 145, 79,
                (byte) 228, (byte) 133, 119, 105, 89, 75, (byte) 234, 66, (byte) 128, (byte) 211, 44, 15, 85, (byte) 191, 98, (byte) 148, 79, 19, 3, (byte) 150,
                (byte) 188, 110, (byte) 155, (byte) 223, 110, (byte) 189, (byte) 210, (byte) 189, (byte) 163, 103, (byte) 142, (byte) 236, (byte) 160, (byte) 198, 104, (byte) 247, 1, (byte) 179,
                (byte) 141, (byte) 191, (byte) 251, 56, (byte) 200, 52, 44, (byte) 226, (byte) 254, 109, 39, (byte) 250, (byte) 222, 74, 90, 72, 116, (byte) 151, (byte) 157,
                (byte) 212, (byte) 185, (byte) 207, (byte) 154, (byte) 222, (byte) 196, (byte) 199, 91, 5, (byte) 133, 44, 44, 15, 94, (byte) 248, (byte) 165, (byte) 193, 117, 3,
                (byte) 146, (byte) 249, 68, (byte) 232, (byte) 237, 100, (byte) 193, 16, (byte) 198, (byte) 182, 71, 96, (byte) 154, (byte) 164, 120, 58, (byte) 235, (byte) 156,
                108, (byte) 154, (byte) 215, 85, 49, 48, 80, 99, (byte) 139, (byte) 131, 102, 92, 111, 111, 122, (byte) 130, (byte) 163, (byte) 150, 112,
                42, 31, 100, 27, (byte) 130, (byte) 211, (byte) 235, (byte) 242, 57, 34, 25, 73, 31, (byte) 182, (byte) 134, (byte) 135, 44, 87, 22, (byte) 245,
                10, (byte) 248, 53, (byte) 141, (byte) 154, (byte) 139, (byte) 157, 23, (byte) 195, 64, 114, (byte) 143, 127, (byte) 135, (byte) 216, (byte) 154, 24, (byte) 216,
                (byte) 252, (byte) 171, 103, (byte) 173, (byte) 132, 89, 12, 46, (byte) 207, 117, (byte) 147, 57, 54, 60, 7, 3, 77, 111, 96, 111,
                (byte) 158, 33, (byte) 224, 84, 86, (byte) 202, (byte) 229, (byte) 233, (byte) 161
        };
        byte[] exponent = {1, 0, 1};
        byte[] privateExponent = {
                18, (byte) 174, 113, (byte) 164, 105, (byte) 205, 10, 43, (byte) 195, 126, 82, 108, 69, 0, 87, 31, 29, 97, 117, 29,
                100, (byte) 233, 73, 112, 123, 98, 89, 15, (byte) 157, 11, (byte) 165, 124, (byte) 150, 60, 64, 30, 63, (byte) 207, 47, 44,
                (byte) 211, (byte) 189, (byte) 236, (byte) 136, (byte) 229, 3, (byte) 191, (byte) 198, 67, (byte) 155, 11, 40, (byte) 200, 47, 125, 55, (byte) 151, 103, 31,
                82, 19, (byte) 238, (byte) 216, (byte) 193, 90, 37, (byte) 216, (byte) 213, (byte) 206, (byte) 160, 2, 94, (byte) 227, (byte) 171, 46, (byte) 139, 127, 121,
                33, 111, (byte) 198, 59, (byte) 234, 86, 39, 83, (byte) 180, 6, 68, (byte) 198, (byte) 161, 81, 39, (byte) 217, (byte) 178, (byte) 149, 69, 64,
                (byte) 160, (byte) 187, (byte) 225, (byte) 163, 5, 86, (byte) 152, 45, 78, (byte) 159, (byte) 222, 95, 100, 37, (byte) 241, 77, 75, 113, 52, 65,
                (byte) 181, 93, (byte) 199, 59, (byte) 155, 74, (byte) 237, (byte) 204, (byte) 146, (byte) 172, (byte) 227, (byte) 146, 126, 55, (byte) 245, 125, 12, (byte) 253, 94,
                117, (byte) 129, (byte) 250, 81, 44, (byte) 143, 73, 97, (byte) 169, (byte) 235, 11, (byte) 128, (byte) 248, (byte) 168, 7, 70, 114, (byte) 138, 85,
                (byte) 255, 70, 71, 31, 52, 37, 6, 59, (byte) 157, 83, 100, 47, 94, (byte) 222, 30, (byte) 132, (byte) 214, 19, 8, 26, (byte) 250,
                92, 34, (byte) 208, 81, 40, 91, (byte) 214, 59, (byte) 148, 59, 86, 93, (byte) 137, (byte) 138, 5, 104, 84, 19, (byte) 229, 60, 60,
                108, 101, 37, (byte) 255, 31, (byte) 227, 78, 61, (byte) 220, 112, (byte) 240, (byte) 213, 100, 80, (byte) 253, (byte) 164, (byte) 139, (byte) 161, 46,
                16, 78, (byte) 157, (byte) 235, (byte) 159, (byte) 184, 24, (byte) 129, (byte) 225, (byte) 196, (byte) 189, (byte) 242, 93, (byte) 146, 71, (byte) 244, 80, (byte) 200, 101,
                (byte) 146, 121, 104, (byte) 231, 115, 52, (byte) 244, 65, 79, 117, (byte) 167, 80, (byte) 225, 57, 84, 110, 58, (byte) 138, 115,
                (byte) 157
        };

        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        try {
            RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(new BigInteger(1, modulus), new BigInteger(1, privateExponent));
            RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));

            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(publicSpec);
            privateKey = kf.generatePrivate(privateSpec);

        } catch (Exception e) {
            fail("Unable to create the RSA Private/Public Keys from supplied bytes.");
        }

        assertNotNull("Public Key is still null", publicKey);
        assertNotNull("Private Key is still null", privateKey);

        // Create the token.
        try {
            jwt.replaceHeader("{\"alg\":\"" + jwt.getSignatureAlgorithmName() + "\"}");
            jwt.setPayload("{\"iss\":\"joe\",\r\n" +
                    " \"exp\":1300819380,\r\n" +
                    " \"http://example.com/is_root\":true}");
            jwt.setSecretAsPrivateKey(privateKey);
            token = jwt.getToken();
        } catch (InvalidJsonException e) {
            fail("The payload is not valid JSON");
        } catch (IOException e) {
            fail("The payload is not valid JSON");
        }

        assertNotNull("No token", token);
        assertTrue("The Appendix A.2 token just created and signed has failed to validate. (Baseline Token)", jwt.validateReceivedToken(token, publicKey));
    }
}
