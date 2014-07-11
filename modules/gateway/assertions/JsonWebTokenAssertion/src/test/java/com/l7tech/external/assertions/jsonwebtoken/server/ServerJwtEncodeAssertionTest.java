package com.l7tech.external.assertions.jsonwebtoken.server;

import com.l7tech.external.assertions.jsonwebtoken.JwtEncodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * These provide the server test functionality using the two required signature
 * methods: none and HS256.
 * <p/>
 * Respective tests for None and HS256 can be found in their test package.
 * Do not add tests for additional signature types to this test.  Add the
 * test class to jsonwebtoken.jsonwebsignature.algorithms in this suite.
 * <p/>
 * This should also attempt to create tokens with invalid signature options
 * as well as invalid payloads, headers, etc.
 * <p/>
 * User: rseminoff
 * Date: 27/11/12
 */
public class ServerJwtEncodeAssertionTest {

    private String header;
    private String payload;
    private String algorithm;

    private JwtEncodeAssertion testAssertion;
    private PolicyEnforcementContext myPec;
    private ApplicationContext appContext;
    private ServerJwtEncodeAssertion testServer;
    private static final Logger log = Logger.getLogger(ServerJwtEncodeAssertionTest.class.getName());

    @Before
    public void setup() {
        testAssertion = new JwtEncodeAssertion();
        header = null;
        payload = null;
        algorithm = null;
        testServer = null;
        myPec = AssertionTestUtil.getBasicPEC();
        appContext = AssertionTestUtil.getTestApplicationContext();
    }

    @Test
    public void testPlainTextJWTFromVariable() {
        payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
        algorithm = "none";
        header = "{\"alg\":\"" + algorithm + "\"}";
        String valueToCompare = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.";

        myPec.setVariable("header", header);
        myPec.setVariable("payload", payload);
        myPec.setVariable("algorithm", algorithm);

        testAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        testAssertion.setJwtHeaderVariable("header");
        testAssertion.setJsonPayload("${payload}");
        testAssertion.setSignatureValue("${algorithm}");
        testAssertion.setSignatureSelected(SELECTED_SIGNATURE_VARIABLE);
        testAssertion.setOutputVariable("output");

        AssertionStatus status;
        try {
            testServer = new ServerJwtEncodeAssertion(testAssertion, appContext);
            status = testServer.checkRequest(myPec);
            assertEquals("Assertion Failed creating plaintext JWT Token", status, AssertionStatus.NONE);

            // Now we're checking output for it's result Value.
            Object output = myPec.getVariable("output");
            if (output instanceof String) {
                assertEquals("Assertion comparison failed", output, valueToCompare);
            } else {
                fail("Assertion returned an unexpected value type");
            }
        } catch (Exception e) {
            fail("Test caused an Exception: " + e.getMessage());
        }
    }

    @Test
    public void testPlainTextJWTFromList() {
        payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
        algorithm = "none";
        header = "{\"alg\":\"" + algorithm + "\"}";
        String valueToCompare = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.";

        myPec.setVariable("header", header);
        myPec.setVariable("payload", payload);
        // Algorithm is not specified by a variable in this test, so it's
        // not included.

        testAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        testAssertion.setJwtHeaderVariable("header");
        testAssertion.setJsonPayload("${payload}");
        testAssertion.setSignatureValue(algorithm);
        testAssertion.setSignatureSelected(SELECTED_SIGNATURE_LIST); // No variable.
        testAssertion.setOutputVariable("output");

        AssertionStatus status;
        try {
            testServer = new ServerJwtEncodeAssertion(testAssertion, appContext);
            status = testServer.checkRequest(myPec);
            assertEquals("Assertion Failed creating plaintext JWT Token", status, AssertionStatus.NONE);

            // Now we're checking hexOut for it's result Value.
            Object output = myPec.getVariable("output");
            if (output instanceof String) {
                assertEquals("Assertion comparison failed", output, valueToCompare);
            } else {
                fail("Assertion returned an unexpected value type");
            }
        } catch (Exception e) {
            fail("Test caused an Exception: " + e.getMessage());
        }
    }

    // This test selects an empty variable secret - because the algorithm is NONE it should pass.
    @Test
    public void testPlainTextJWTFromVariableWithEmptyVariableSecret() {
        payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
        algorithm = "none";
        header = "{\"alg\":\"" + algorithm + "\"}";
        String valueToCompare = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.";

        myPec.setVariable("header", header);
        myPec.setVariable("payload", payload);
        myPec.setVariable("algorithm", algorithm);

        testAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        testAssertion.setJwtHeaderVariable("header");
        testAssertion.setJsonPayload("${payload}");
        testAssertion.setSignatureSelected(SELECTED_SIGNATURE_VARIABLE);
        testAssertion.setSignatureValue("${algorithm}");
        testAssertion.setOutputVariable("output");
        testAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);
        testAssertion.setAlgorithmSecretValue("");

        AssertionStatus status;
        try {
            testServer = new ServerJwtEncodeAssertion(testAssertion, appContext);
            status = testServer.checkRequest(myPec);
            assertEquals("Assertion Failed creating plaintext JWT Token", status, AssertionStatus.NONE);

            // Now we're checking output for it's result Value.
            Object output = myPec.getVariable("output");
            if (output instanceof String) {
                assertEquals("Assertion comparison failed", output, valueToCompare);
            } else {
                fail("Assertion returned an unexpected value type");
            }
        } catch (Exception e) {
            fail("Test caused an Exception: " + e.getMessage());
        }
    }

    @Test
    public void testHS256SignedJWTFromVariable() {
        payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
        algorithm = "HS256";
        header = "{\"typ\":\"JWT\",\r\n \"alg\":\"" + algorithm + "\"}";
        String valueToCompare = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        byte[] secretBytes = new byte[]{3, 35, 53, 75, 43, 15, (byte) 165, (byte) 188, (byte) 131, 126, 6, 101, 119, 123, (byte) 166, (byte) 143, 90, (byte) 179, 40, (byte) 230, (byte) 240, 84, (byte) 201, 40, (byte) 169, 15, (byte) 132, (byte) 178, (byte) 210, 80, 46, (byte) 191, (byte) 211, (byte) 251, 90, (byte) 146, (byte) 210, 6, 71, (byte) 239, (byte) 150, (byte) 138, (byte) 180, (byte) 195, 119, 98, 61, 34, 61, 46, 33, 114, 5, 46, 79, 8, (byte) 192, (byte) 205, (byte) 154, (byte) 245, 103, (byte) 208, (byte) 128, (byte) 163};

        myPec.setVariable("header", header);
        myPec.setVariable("payload", payload);
        myPec.setVariable("algorithm", algorithm);
        // Base64 encoding the secret is the only real sane way to pass it in a variable.
        myPec.setVariable("secret", new String(encode(secretBytes)));

        testAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        testAssertion.setJwtHeaderVariable("header");
        testAssertion.setJsonPayload("${payload}");
        testAssertion.setSignatureValue("${algorithm}");
        testAssertion.setSignatureSelected(SELECTED_SIGNATURE_VARIABLE);
        testAssertion.setAlgorithmSecretValue("${secret}");
        testAssertion.setAlgorithmSecretLocation(SELECTED_SECRET_VARIABLE_BASE64);
        testAssertion.setOutputVariable("output");

        AssertionStatus status;
        try {
            testServer = new ServerJwtEncodeAssertion(testAssertion, appContext);
            status = testServer.checkRequest(myPec);
            assertEquals("Assertion Failed creating plaintext JWT Token", status, AssertionStatus.NONE);

            // Now we're checking hexOut for it's result Value.
            Object output = myPec.getVariable("output");
            if (output instanceof String) {
                assertEquals("Assertion comparison failed", output, valueToCompare);
            } else {
                fail("Assertion returned an unexpected value type");
            }
        } catch (Exception e) {
            fail("Test caused an Exception: " + e.getMessage());
        }
    }

    @Test
    public void testHS256SignedJWTFromList() {
        payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
        algorithm = "HS256";
        header = "{\"typ\":\"JWT\",\r\n \"alg\":\"" + algorithm + "\"}";

        String valueToCompare = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        byte[] secretBytes = new byte[]{3, 35, 53, 75, 43, 15, (byte) 165, (byte) 188, (byte) 131, 126, 6, 101, 119, 123, (byte) 166, (byte) 143, 90, (byte) 179, 40, (byte) 230, (byte) 240, 84, (byte) 201, 40, (byte) 169, 15, (byte) 132, (byte) 178, (byte) 210, 80, 46, (byte) 191, (byte) 211, (byte) 251, 90, (byte) 146, (byte) 210, 6, 71, (byte) 239, (byte) 150, (byte) 138, (byte) 180, (byte) 195, 119, 98, 61, 34, 61, 46, 33, 114, 5, 46, 79, 8, (byte) 192, (byte) 205, (byte) 154, (byte) 245, 103, (byte) 208, (byte) 128, (byte) 163};

        myPec.setVariable("header", header);
        myPec.setVariable("payload", payload);
        // Base64 encoding the secret is the only real sane way to pass it in a variable.
        myPec.setVariable("secret", new String(encode(secretBytes)));

        testAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        testAssertion.setJwtHeaderVariable("header");
        testAssertion.setJsonPayload("${payload}");
        testAssertion.setSignatureValue(algorithm);
        testAssertion.setSignatureSelected(SELECTED_SIGNATURE_LIST);
        testAssertion.setAlgorithmSecretValue("${secret}");
        testAssertion.setAlgorithmSecretLocation(SELECTED_SECRET_VARIABLE_BASE64);
        testAssertion.setOutputVariable("output");

        AssertionStatus status;
        try {
            testServer = new ServerJwtEncodeAssertion(testAssertion, appContext);
            status = testServer.checkRequest(myPec);
            assertEquals("Assertion Failed creating plaintext JWT Token", status, AssertionStatus.NONE);

            // Now we're checking hexOut for it's result Value.
            Object output = myPec.getVariable("output");
            if (output instanceof String) {
                assertEquals("Assertion comparison failed", output, valueToCompare);
            } else {
                fail("Assertion returned an unexpected value type");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test caused an Exception: " + e.getMessage());
        }
    }

    // This test selects an empty variable secret - because the algorithm is HS256 it should fail.
    @Test
    public void testHS256SignedJWTWithEmptyVariableSecret() {
        payload = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
        algorithm = "HS256";
        header = "{\"typ\":\"JWT\",\r\n \"alg\":\"" + algorithm + "\"}";

        String valueToCompare = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        byte[] secretBytes = new byte[]{3, 35, 53, 75, 43, 15, (byte) 165, (byte) 188, (byte) 131, 126, 6, 101, 119, 123, (byte) 166, (byte) 143, 90, (byte) 179, 40, (byte) 230, (byte) 240, 84, (byte) 201, 40, (byte) 169, 15, (byte) 132, (byte) 178, (byte) 210, 80, 46, (byte) 191, (byte) 211, (byte) 251, 90, (byte) 146, (byte) 210, 6, 71, (byte) 239, (byte) 150, (byte) 138, (byte) 180, (byte) 195, 119, 98, 61, 34, 61, 46, 33, 114, 5, 46, 79, 8, (byte) 192, (byte) 205, (byte) 154, (byte) 245, 103, (byte) 208, (byte) 128, (byte) 163};

        myPec.setVariable("header", header);
        myPec.setVariable("payload", payload);
        // Base64 encoding the secret is the only real sane way to pass it in a variable.
        myPec.setVariable("secret", new String(encode(secretBytes)));

        testAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        testAssertion.setJwtHeaderVariable("header");
        testAssertion.setJsonPayload("${payload}");
        testAssertion.setSignatureValue(algorithm);
        testAssertion.setSignatureSelected(SELECTED_SIGNATURE_LIST);
        testAssertion.setOutputVariable("output");
        testAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);
        testAssertion.setAlgorithmSecretValue("");

        AssertionStatus status;
        try {
            testServer = new ServerJwtEncodeAssertion(testAssertion, appContext);
            status = testServer.checkRequest(myPec);
            assertEquals("Assertion Failed creating HS256 JWT Token with an empty selected secret.  This is a PASS.", status, AssertionStatus.FAILED);
        } catch (Exception e) {
            fail("Test caused an Exception: " + e.getMessage());
        }
    }

}
