package com.l7tech.external.assertions.jsonwebtoken.server;

import com.l7tech.external.assertions.jsonwebtoken.JwtDecodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.JwtEncodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * User: rseminoff
 * Date: 25/04/13
 */
public class MagRegressionTest {

    // These tests contain the bugs that were filed against 1.2.2 of JWT Module.
    // As the codebase moves forward, this is to ensure the bugs don't pop up again.

    private JwtEncodeAssertion encodeAssertion;
    private ServerJwtEncodeAssertion serverEncodeAssertion;
    private JwtDecodeAssertion decodeAssertion;
    private ServerJwtDecodeAssertion serverDecodeAssertion;

    private PolicyEnforcementContext testPec;
    private ApplicationContext testAppContext;

    @Before
    public void setup() {
        testPec = AssertionTestUtil.getBasicPEC();
        testAppContext = AssertionTestUtil.getTestApplicationContext();
    }

    @Test
    /**
     * MAG-63: Encode JWT assertion - Leaving JSON payload field empty causes exception to be thrown during run time
     * Expected Result: The JSON Payload field show result in an error, not an exception.
     */
    public void MAG_63() {

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload(""); // No payload variable defined.  That's the bug.
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "none");   // No Signature Algorithm

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an unassigned payload variable", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }

    }

    @Test
    /**
     * Encode JWT assertion - Assertion should fail when no 'output variable' specified
     * Expected Result: According to the Func. spec, the assertion should fail
     *                  https://wiki.l7tech.com/mediawiki/index.php/JWT_Tactical_Assertion_Module
     */
    public void MAG_64() {

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable(""); // No output variable.  This is the bug.

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}"); // Standard Payload

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an unassigned output variable", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }

    }

    @Test
    /**
     * Encode JWT assertion - Exception is thrown when JSON payload variable is set to Integer or Date/Time format
     * Expected: Assertion should fail gracefully
     */
    public void MAG_65_Case_Integer() {

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("output");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
//        testPec.setVariable("payload", new Integer(1234)); // Non-String Payload, that's the bug.
        testPec.setVariable("payload", 1234); // Non-String Payload, that's the bug.

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an Integer payload", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }

    }

    @Test
    /**
     * Encode JWT assertion - Exception is thrown when JSON payload variable is set to Integer or Date/Time format
     * Expected: Assertion should fail gracefully
     */
    public void MAG_65_Case_DateTime() {

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("output");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", new Date(1234)); // Non-String Payload, that's the bug.

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with a Date/Time payload", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }

    }

    @Test
    /**
     * Encode JWT assertion - Exception is thrown when JSON payload is set to empty string
     * Expected: Assertion should fail gracefully.
     */
    public void MAG_66() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", ""); // Empty Payload, that's the bug.

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an empty claims payload", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

//    @Test -- THIS IS CURRENTLY UNTESTABLE IN CODE AND MUST BE DONE AT SSG RUNTIME VIA POLICY
    /**
     * Encode JWT assertion - Assertion should fail when 'expired' private key is used for algorithm secret
     * Expected: Assertion should fail gracefully
     */
    public void MAG_67() {
        String expiredKeyBase64 = "MIIJIAIBAzCCCOYGCSqGSIb3DQEHAaCCCNcEggjTMIIIzzCCBa8GCSqGSIb3DQEHBqCCBaAwggWc\n" +
                "AgEAMIIFlQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQISxYTNQVC2xECAggAgIIFaKJE50F5\n" +
                "gTco2qT6Jl6XC8v54hLtcWxuU5/OwYC1LAixwTnrG4kwhRsVZiTZeRmxDaGxkbCuf7GsYhaY9jo7\n" +
                "fSrjGekTNpAlGajDqiugFTKkdbfbEo6YxaWRhOZYr5l5hhotAzy+nitapca9e43zVyzlHxJj4qph\n" +
                "oiobRQ8se3qN9Rol73B5UTnt9yVib6J22P3vhiowHdaMrB4Rmbc+xjAXNstGsL4A5pI6RGzUWaX1\n" +
                "+8D5vaKjigJqGE8Cyi58LZMVUGfIkLAc7IhwwlEA3PdwBbvBAMNwsSbCXpVjjmOyMfaaB/Dl4MQ9\n" +
                "rsPDNIe5ev8U9Ns5WqNpTgw+owhFgC+7cC2gJDKsfb5lHDbLzTB1X1x4k/t5xHFXxLoCS2y2b6Wj\n" +
                "WWlzRkrk1x6L+DOC/plVSWqPEuP+k2tJ0mk2qy4e6QApMEjxiE6SF1PmLMxDYJTuwRz0CHco4Bog\n" +
                "S3zaMMYQSIabBui4kfXVS9bZjUMmDmJoONQdgAwNlxGlebe9NZSV6Jr5nuLlcLc0cdDG4lF0TBpx\n" +
                "I/R6yGgAq3TJYTgltt3toe3e4L5oZwj8F8kYUneBLhab8AaQHBemac9kKck4FpH9FM2wPxpUe31b\n" +
                "vte79xmWk2TRMNzWpi6Ve+pe1CaC7igdLU+IxFgWnZ/KYglNC3vTyfwX1IaJwpAdAlHoO9Ty5yBC\n" +
                "Hf4qEtaYqx4HY7Y5aIwMxM9QMN6VDcuNecLJrez1w3l6It6/D6CqMKZiX++T6/zap7cPcKTNlsx+\n" +
                "0+GehqU4ZKoFdGGfVxqZF4LsCTYi5aovM8BFYZjsoLX+f11ioebR8agxRCYLmbEpkkYg8FcM/QyK\n" +
                "mFb345v5SrTVTN7brlfvjJ5Y7+81TxtL7ovrsqL91ZQcoJ4zdyoG6SVMyCqW6usFl4+JMgOae3lK\n" +
                "pVUtxbkjcsclJa55ceyLU3ZUheOCYJRf7Nuv4zEH2J9YZ8Z9uZ+QYjurapH9jsgz1NwXM/z0F3GK\n" +
                "QqvM7RnhEZQOAth2L2ZY8Z1pf0/hDPZ5ZmURUxIKw47eod+0lAJlY7UjSinTgE0Lz7G+VDHuRr7i\n" +
                "ZCc6S5a76ykcjYlw/v5t7Y6W4+sBLA+7wGsbZ4SQbG1MruI6o13l8PbImU5bch0ySGhZbcUHhd9N\n" +
                "oVw+qnpGkn3RPVomPuSriB0SD71IFBM0EdacUCgjv7zjUafITRs6dLofvR7lb3mGpPAYI+aHNcZK\n" +
                "rHfA/PlCGqqPvyMFlP8/eLpKMbtWORIspUg4+3bVBCNZcZAy+Ovw4JHSls5RWOgRHjThg3UA9Y8+\n" +
                "kKW8nEsKisxxOgTTupJq8SmL0zf4pdGDXEyeEvwXULN2+VkXR8ztK6aJ3Ug4fFz53NtJ9j0DTg/Y\n" +
                "b5HDyS3SozdAV4Pf9uewE4OJ00N6kqaxvYtouADVAJYDisw8CYu0m3LbwvnNXdafAvFNrciv9HmY\n" +
                "BMGDq/S1jr7eXc7Qf0EMLBpozEGOEt2jYPVCrhW0ix1Usud0/DDkv9Eos5EhSe+dKT3wTt10e3TP\n" +
                "UzHRJwxCJDxI3xzUapAbSBckpXUJFP3XeH+75Xbu0ODWuUEatLg0WgPVF+ET54bIOYfGhNMC6Mc/\n" +
                "upgUM/tikE4xTlv2LU77+2L2ErZ5RqO7paMOuFnE+DljeKPx3Yc7k3GqbvZpt7C4BkkBdSTej/sW\n" +
                "fWvd8QcgrSAbGZd5FkAdkvNnEl8R8WDRV3u4eIlOVh7c0IA/slsFRzusQ2ZQuweYsZb847wjjtlJ\n" +
                "3dg71Lj98y3LI+yyLdOdfgetTKdjUjqbltDX4K7wB2A+eTy7xCTs+fuBhjDYNpLj1UOtUbBZiTmV\n" +
                "/E2U01mbA1Uj/EUwggMYBgkqhkiG9w0BBwGgggMJBIIDBTCCAwEwggL9BgsqhkiG9w0BDAoBAqCC\n" +
                "AqYwggKiMBwGCiqGSIb3DQEMAQMwDgQI0yBryM8XUywCAggABIICgFbL3SUhsFrRdrIZZPHk42uV\n" +
                "x55sUkCJ5JoUy7Ba2+9QU2gM9SbaH9f6bgUrKNFpn1trmTALQbU7Or6P2QATEbJluDZYeo7d9Lr9\n" +
                "VjhhKauNYo+k7FMthzgZHvUoIApUSYn17FOLncJ4tCZ2JAwqhNE97hIGqwIge9rz/jsmGxI3R+1E\n" +
                "acg/6jz5kf4joAKS9pXx3/Ro1YXm4ttPtrKqwfWUIdvHHesbMnXie2XbOqPNV3b+M3O61Xw40azQ\n" +
                "1FBTL0TtrbzGCxyBbL/myx280hH4pp51WN7R3NlxHqmkU4iLvbXP2uAQLmYLOgGgaiXQQKdvMHIH\n" +
                "Y8XX1Rn+uwGL52h3xpsl1SK/hKpjI0aDZha901LLHigIZ577zNBJvXqRQLOH2IDlJlyXm5CrwT3L\n" +
                "o4vIC/mqgMJ+rwPUtseGP5cLHlJqdREy4qV5vPCIeH/2fPw49wc41xhJhU/BCmoNk+oK7VgQm4EU\n" +
                "UwKdJu00U04RgBT9z5MRMQiOYhXelBSokPH5+FBzQpLODYdYrCYUQG/uunsDCVO6Lh7X2VMXryud\n" +
                "5psIBIQgY4DEc2xrZ78GH4YuXhJ934BAtMymMoEZlJt47hNbbtLqZvAz/LQuMY0QoxCG4voWx+eY\n" +
                "9iXp7Vq/HvpaxkEHk9C8/rN+J7gQFNwrE7o0KYKDSgUPt5cwMreOe2v9PRlV4lj3vwC8zCbfhllW\n" +
                "8Jpyw4xE7HgygZ8OneppFBVujyt9tWasVflyVpbbKu9/17HtMhsvv6vnX3hY9GC7F0V/+hX8hT6T\n" +
                "IY9V/bZ4wJe5ZY37wlTlk+nY9K8atJzP4D1YMyozn6/ohwhUkAf9kivJupGhbkfSYcWXqVoxRDAd\n" +
                "BgkqhkiG9w0BCRQxEB4OAGEAZwBlAEMAZQByAHQwIwYJKoZIhvcNAQkVMRYEFP/QXZM+C9CsoUMa\n" +
                "S7SSAIoA4MVAMDEwITAJBgUrDgMCGgUABBRPcEUYNilWVOzem3g5t4eh+RCc/gQIQZyrsZ8Tp68C\n" +
                "AggA";

        ByteArrayInputStream bais = new ByteArrayInputStream(org.apache.commons.codec.binary.Base64.decodeBase64(expiredKeyBase64));
        PrivateKey pk;
        try {
            KeyStore ks = KeyStore.getInstance("pkcs12");
            ks.load(bais, "password".toCharArray());
            System.out.println("*** PRIVATE KEY LOADED, STORE CONTAINS " + ks.size() + " ENTRIES ***");
            System.out.println("Entries Are:");
            Enumeration<String> certAliases = ks.aliases();
            while (certAliases.hasMoreElements()) {
                System.out.println("    -> " + certAliases.nextElement());
            }
            pk = (PrivateKey)ks.getKey("agecert", "password".toCharArray());
        } catch (KeyStoreException e) {
            fail("An exception occurred getting the Keystore");
        } catch (CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
            fail("An exception occurred processing the expired key");
        }


        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64);  // Secret is in a variable whose contents are base64 encoded
        encodeAssertion.setAlgorithmSecretValue("${secret}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "RS256");   // RSA SHA-256, requires private key
        testPec.setVariable("payload", "{\"iss\":1234}"); // Simple payload
        testPec.setVariable("secret", "");  // This contains the base64 Encoded p12 of an expired private key

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an empty payload", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     * Encode JWT assertion - Exception is thrown when 'generated header' variable is set to string-with-no-brackets, message, Integer or Date/Time format
     * Expected: Assertion should fail gracefully
     */
    public void MAG_68_Case_String_No_Brackets() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setJwtHeaderVariable("header");
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("header", "\"iss\":1234");  // No curly brackets for a full header.  This is required for a full header.
        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}");

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an invalid replacement header (No {} braces)", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

//    @Test  -- DISABLED FOR THE TIME BEING.
    /**
     * Encode JWT assertion - Exception is thrown when 'generated header' variable is set to string-with-no-brackets, message, Integer or Date/Time format
     * Expected: Assertion should fail gracefully
     */
    public void MAG_68_Case_Message() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setJwtHeaderVariable("header");
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token");

//        testPec.setVariable("header", new Message(new ));
//        }"\"iss\":1234"));  // No curly brackets for a full header.  This is required for a full header.
        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}");

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an invalid replacement header (Message)", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     * Encode JWT assertion - Exception is thrown when 'generated header' variable is set to string-with-no-brackets, message, Integer or Date/Time format
     * Expected: Assertion should fail gracefully
     */
    public void MAG_68_Case_Integer() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setJwtHeaderVariable("header");
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("header", 1234);  // Use an Integer for the header.  That's the bug.
        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}");

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an invalid replacement header (Integer)", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     * Encode JWT assertion - Exception is thrown when 'generated header' variable is set to string-with-no-brackets, message, Integer or Date/Time format
     * Expected: Assertion should fail gracefully
     */
    public void MAG_68_Case_DateTime() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setJwtHeaderVariable("header");
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("header", new Date());  // Use a DateTime for the header.  That's the bug.
        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "\"iss\":1234");

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an invalid replacement header (Date/Time)", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     * Encode JWT assertion - Encoded value of 'use generated header' should be the same as 'append to generated header' with [empty] variable
     * Expected: Encoded value should be : eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJub25lIn0.
     */
    public void MAG_69() {
        String payload1 = null, payload2 = null;
        AssertionStatus status;

        // Perform the first token operation.
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}");

        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            serverEncodeAssertion.checkRequest(testPec);

            Object obj = testPec.getVariable("token");
            if (obj instanceof String) {
                payload1 = (String)obj;
            } else {
                fail("The test returned a token variable containing an unexpected type");
            }
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        } catch (NoSuchVariableException e) {
            fail("The test threw an exception, as the first payload token variable doesn't exist");
        } catch (Exception e) {
            fail("The test threw an exception generating the first token, and was stopped.");
        }

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_PARTIAL_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setJwtHeaderVariable("header");
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);  // No Secret
        encodeAssertion.setOutputVariable("token2");

        testPec.setVariable("header", "");  // Empty header.  This will eventually be an error.
        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}");

        status = AssertionStatus.NONE;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an empty appended header", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     * Encode JWT assertion - Exception is thrown when algorithm secret variable contains less than 4 chars
     * Expected: Assertion should fail gracefully.
     */
    public void MAG_70_Case_StringLessThan4Chars() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE_BASE64);  // Variable contains a secret, that IS base64 encoded
        encodeAssertion.setAlgorithmSecretValue("${secret}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("secret", "abc");   // Set the secret to something < 4 chars.  That's the bug.
        testPec.setVariable("payload", "{\"iss\":1234}");

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with a base64 encoded secret of less than three characters (abc)", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     *  Encode JWT & Decode JWT assertion - Assertion should check if header type is correct before performing encoding/decoding process
     *  Expected: Assertion should now allow encoding as "typ" is incorrect.
     */
    public void MAG_71_Case_Encode_BadType() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.SUPPLIED_FULL_JWT_HEADER);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_NONE);
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);

        encodeAssertion.setJwtHeaderVariable("header");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("header", "{\"typ\":\"XML\", \"alg\":\"none\"}");  // Set an invalid type in the replacement header.  That's the bug.
        testPec.setVariable("payload", "{\"iss\":1234}");

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an invalid token type in the replacement header", AssertionStatus.fromInt(601), status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     *  Encode JWT & Decode JWT assertion - Assertion should check if header type is correct before performing encoding/decoding process
     *  Expected: Assertion should now allow encoding as "typ" is incorrect.
     */
    public void MAG_71_Case_Decode_BadType() {
        decodeAssertion = new JwtDecodeAssertion();

        decodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_NONE);

        decodeAssertion.setOutputVariable("tokenOut");
        decodeAssertion.setIncomingToken("token");

        testPec.setVariable("token", "eyJ0eXAiOiJYTUwiLCAiYWxnIjoibm9uZSJ9.eyJpc3MiOjEzNDV9.");   // A token with an invalid type in the header.  This is the bug.

        AssertionStatus status;
        try {
            serverDecodeAssertion = new ServerJwtDecodeAssertion(decodeAssertion, testAppContext);
            status = serverDecodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with an invalid token type in the replacement header", AssertionStatus.fromInt(601), status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     *  Encode/Decode JWT assertion - 'JWT decoding assertion' fails to decode the token that successfully encode by 'JWT encoding assertion'
     *  Expected: Encoding should fail
     */
    public void MAG_72_Case_Encoder_BadSecret_Integer() {

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);  // Secret is in the variable
        encodeAssertion.setAlgorithmSecretValue("${secret}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}"); // Standard Payload
        testPec.setVariable("secret", new Integer(12345)); // Integer Secret - Bug

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with Integer Type Secret", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     *  Encode/Decode JWT assertion - 'JWT decoding assertion' fails to decode the token that successfully encode by 'JWT encoding assertion'
     *  Expected: Encoding should fail
     */
    public void MAG_72_Case_Encoder_BadSecret_DateTime() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);  // Secret is in the variable
        encodeAssertion.setAlgorithmSecretValue("${secret}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}"); // Standard Payload
        testPec.setVariable("secret", new Date());  // Date/Time Secret - Bug.

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with DateTime Type Secret", AssertionStatus.FAILED, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

    @Test
    /**
     *  Encode/Decode JWT assertion - 'JWT decoding assertion' fails to decode the token that successfully encode by 'JWT encoding assertion'
     *  Expected: Encoding should fail
     */
    public void MAG_72_Case_Encoder_GoodSecret_String() {
        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);  // Secret is in the variable
        encodeAssertion.setAlgorithmSecretValue("${secret}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "none");   // No Signature Algorithm
        testPec.setVariable("payload", "{\"iss\":1234}"); // Standard Payload
        testPec.setVariable("secret", new String("password"));  // String Secret - this is normal.

        AssertionStatus status;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            status = serverEncodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully with String Type Secret", AssertionStatus.NONE, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }


//    @Test  -- UNNECESSARY FOR THIS VERSION OF THE ASSERTION, as the current encode assertion cannot create tokens that contain base64url encoded payloads.
    /**
     * JWT Decode should validate payload before decoding and passing it back
     * Expected: Either fully validated and decoded JSON, OR the original payload in base64URL encoding.
     */
    public void MAG_73_Case_JSONPayload() {
        // This needs a legit token.
        decodeAssertion = new JwtDecodeAssertion();

        decodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);

        decodeAssertion.setOutputVariable("tokenOut");
        decodeAssertion.setIncomingToken("token");
        decodeAssertion.setAlgorithmSecretValue("${secret}");

        // Provide a valid token with a JSON payload.
        testPec.setVariable("token", "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOjEzNDV9.pylJaVxvK6FeKnUYTkIbs6fzwTlpBoaaJBwXuzy5H6o");
        testPec.setVariable("secret", "12345"); // What was used to initially sign the token with.

        AssertionStatus status;
        try {
            serverDecodeAssertion = new ServerJwtDecodeAssertion(decodeAssertion, testAppContext);
            status = serverDecodeAssertion.checkRequest(testPec);
            assertEquals("Assertion completed successfully.", AssertionStatus.NONE, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }
    }

//    @Test -- UNTESTABLE - Can't generate a valid token with an existing encoded payload with this version of the assertion.
    /**
     * JWT Decode should validate payload before decoding and passing it back
     * Expected: Either fully validated and decoded JSON, OR the original payload in base64URL encoding.
     */
    public void MAG_73_Case_OtherPayload() {
        // This needs a legit token.

    }

//    @Test -- NOT TO USE USED FOR 1.X OF THE JWT MODULE
    /**
     * Encode/Decode JWT assertion - Decoder fails when token contains PEM cert in the algorithm secret
     * Expected: Decoding should pass
     */
    public void MAG_74_Case_Plaintext_PEM_Password() {
        // Needs a PEM Signed Token
        String pemSecret = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDBjCCAe6gAwIBAgIIb0Hq9pdjw7swDQYJKoZIhvcNAQEMBQAwITEfMB0GA1UEAxMWcnNlbWlu\n" +
                "b2ZmLmw3dGVjaC5sb2NhbDAeFw0xMjExMjExNTUzMjJaFw0yMjExMTkxNTUzMjJaMCExHzAdBgNV\n" +
                "BAMTFnJzZW1pbm9mZi5sN3RlY2gubG9jYWwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\n" +
                "AQC9poIWvfX3hoXslboRJZ4ZVM8Ui27zh7wqkR/eZZtk7OBV42Tfxj9o+qFJDtaKLiasY5PxYjZ5\n" +
                "H0izl4ybvxMHQhUaN9+HLkAKMs3A99S8i4Hf9uWJizY5MJ854fDGD6Vi6ew0Y5hj4V3/e4XjYbhC\n" +
                "v2X9f/hIZTOJTkk9uZUH4XlfsxRFLDea3j3y8OMHF/qdfna/vGUIwXQ5BYXkn4kg9aVHvWerXO6x\n" +
                "Xm3aRFBiLxOt4toqbD7DOltgzcxkzeLAqAhOhgX/jg1ZLzzrjVbSowMydSS318Z4VbNGMXhderIZ\n" +
                "OeB1pesJyymttGk56ObDqsgzh8m9GT04P780DUgHAgMBAAGjQjBAMB0GA1UdDgQWBBTvpgOxlTBH\n" +
                "9lY2i/15XEkpl34n7DAfBgNVHSMEGDAWgBTvpgOxlTBH9lY2i/15XEkpl34n7DANBgkqhkiG9w0B\n" +
                "AQwFAAOCAQEAEUSwuglsrJXN984Hwc0r9VqaPADj/K0sfDOwHHNiYvsY6KPWNuahJCl99wb/eECY\n" +
                "Yc/o8maltEgD/O1pdLrOeqdniXLUDF6ENTlH09ib3EYP8aRoFg3S+2lTNJnjFQkTUq1MFn2HFpDn\n" +
                "vRYI3RnpgjWvNbVUQLahqYh9XTQ8JFlXQifw6BALpK8+ADjIS3Pf6ft7PyBqIOPhqfMj5Q4gXayb\n" +
                "sULMKnRtRD9gUbKHdFwnl0GIrDCJr+SR2k4wNUWwH86qgmkx9nnEb8eEfGfGpGHpFiU9nT13iVaM\n" +
                "W3eWWNo2ii0HuPr7QyKEZ4YHWqzPTU6ddENQMe6UtWcpRTeV5w==\n" +
                "-----END CERTIFICATE-----\n";

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);  // Secret is in the variable
        encodeAssertion.setAlgorithmSecretValue("${secret}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "HS256");   // Sign with HS256
        testPec.setVariable("payload", "{\"iss\":1234}"); // Standard Payload
        testPec.setVariable("secret", pemSecret);  // Use the PEM Cert as the secret

        String token = null;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            serverEncodeAssertion.checkRequest(testPec);
            Object tokenTemp = testPec.getVariable("token");
            if (tokenTemp instanceof String) {
                token = (String) tokenTemp;
            }
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("The test threw an unexpected IOException. (" + e.getMessage() + ")");
        } catch (NoSuchVariableException e) {
            fail("The test threw an exception - the encoded token output variable doesn't exist");
        }


        // This needs a legit token.
        decodeAssertion = new JwtDecodeAssertion();

        decodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);

        decodeAssertion.setOutputVariable("tokenOut");
        decodeAssertion.setIncomingToken("token");
        decodeAssertion.setAlgorithmSecretValue("${secret}");

        // Provide a valid token with a JSON payload.
        testPec.setVariable("token", token);
        testPec.setVariable("secret", pemSecret); // What was used to initially sign the token with.

        AssertionStatus status;
        try {
            serverDecodeAssertion = new ServerJwtDecodeAssertion(decodeAssertion, testAppContext);
            status = serverDecodeAssertion.checkRequest(testPec);
            assertEquals("Assertion failed decoding a PEM-signed token.", AssertionStatus.NONE, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }

    }

//    @Test -- NOT TO USE USED FOR 1.X OF THE JWT MODULE
    /**
     * Encode/Decode JWT assertion - Decoder fails when token contains PEM cert in the algorithm secret
     * Expected: Decoding should pass
     */
    public void MAG_74_Case_Base64Encoded_PEM_Password() {
        // Needs a Base64 Encoded PEM Signed Token
        String pemSecret = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURCakNDQWU2Z0F3SUJBZ0lJYjBIcTlwZGp3\n" +
                "N3N3RFFZSktvWklodmNOQVFFTUJRQXdJVEVmTUIwR0ExVUVBeE1XY25ObGJXbHUNCmIyWm1MbXcz\n" +
                "ZEdWamFDNXNiMk5oYkRBZUZ3MHhNakV4TWpFeE5UVXpNakphRncweU1qRXhNVGt4TlRVek1qSmFN\n" +
                "Q0V4SHpBZEJnTlYNCkJBTVRGbkp6WlcxcGJtOW1aaTVzTjNSbFkyZ3ViRzlqWVd3d2dnRWlNQTBH\n" +
                "Q1NxR1NJYjNEUUVCQVFVQUE0SUJEd0F3Z2dFS0FvSUINCkFRQzlwb0lXdmZYM2hvWHNsYm9SSlo0\n" +
                "WlZNOFVpMjd6aDd3cWtSL2VaWnRrN09CVjQyVGZ4ajlvK3FGSkR0YUtMaWFzWTVQeFlqWjUNCkgw\n" +
                "aXpsNHlidnhNSFFoVWFOOStITGtBS01zM0E5OVM4aTRIZjl1V0ppelk1TUo4NTRmREdENlZpNmV3\n" +
                "MFk1aGo0VjMvZTRYalliaEMNCnYyWDlmL2hJWlRPSlRrazl1WlVINFhsZnN4UkZMRGVhM2ozeThP\n" +
                "TUhGL3FkZm5hL3ZHVUl3WFE1QllYa240a2c5YVZIdldlclhPNngNClhtM2FSRkJpTHhPdDR0b3Fi\n" +
                "RDdET2x0Z3pjeGt6ZUxBcUFoT2hnWC9qZzFaTHp6cmpWYlNvd015ZFNTMzE4WjRWYk5HTVhoZGVy\n" +
                "SVoNCk9lQjFwZXNKeXltdHRHazU2T2JEcXNnemg4bTlHVDA0UDc4MERVZ0hBZ01CQUFHalFqQkFN\n" +
                "QjBHQTFVZERnUVdCQlR2cGdPeGxUQkgNCjlsWTJpLzE1WEVrcGwzNG43REFmQmdOVkhTTUVHREFX\n" +
                "Z0JUdnBnT3hsVEJIOWxZMmkvMTVYRWtwbDM0bjdEQU5CZ2txaGtpRzl3MEINCkFRd0ZBQU9DQVFF\n" +
                "QUVVU3d1Z2xzckpYTjk4NEh3YzByOVZxYVBBRGovSzBzZkRPd0hITmlZdnNZNktQV051YWhKQ2w5\n" +
                "OXdiL2VFQ1kNClljL284bWFsdEVnRC9PMXBkTHJPZXFkbmlYTFVERjZFTlRsSDA5aWIzRVlQOGFS\n" +
                "b0ZnM1MrMmxUTkpuakZRa1RVcTFNRm4ySEZwRG4NCnZSWUkzUm5wZ2pXdk5iVlVRTGFocVloOVhU\n" +
                "UThKRmxYUWlmdzZCQUxwSzgrQURqSVMzUGY2ZnQ3UHlCcUlPUGhxZk1qNVE0Z1hheWINCnNVTE1L\n" +
                "blJ0UkQ5Z1ViS0hkRndubDBHSXJEQ0pyK1NSMms0d05VV3dIODZxZ21reDlubkViOGVFZkdmR3BH\n" +
                "SHBGaVU5blQxM2lWYU0NClczZVdXTm8yaWkwSHVQcjdReUtFWjRZSFdxelBUVTZkZEVOUU1lNlV0\n" +
                "V2NwUlRlVjV3PT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=";

        encodeAssertion = new JwtEncodeAssertion();
        encodeAssertion.setJwtHeaderType(JwtUtilities.NO_SUPPLIED_HEADER_CLAIMS);
        encodeAssertion.setSignatureSelected(JwtUtilities.SELECTED_SIGNATURE_VARIABLE);
        encodeAssertion.setSignatureValue("${signature}");
        encodeAssertion.setJsonPayload("${payload}");
        encodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);  // Secret is in the variable
        encodeAssertion.setAlgorithmSecretValue("${secret}");
        encodeAssertion.setOutputVariable("token");

        testPec.setVariable("signature", "HS256");   // Sign with HS256
        testPec.setVariable("payload", "{\"iss\":1234}"); // Standard Payload
        testPec.setVariable("secret", pemSecret);  // Use the PEM Cert as the secret

        String token = null;
        try {
            serverEncodeAssertion = new ServerJwtEncodeAssertion(encodeAssertion, testAppContext);
            serverEncodeAssertion.checkRequest(testPec);
            Object tokenTemp = testPec.getVariable("token");
            if (tokenTemp instanceof String) {
                token = (String) tokenTemp;
            }
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("The test threw an unexpected IOException. (" + e.getMessage() + ")");
        } catch (NoSuchVariableException e) {
            fail("The test threw an exception - the encoded token output variable doesn't exist");
        }


        // This needs a legit token.
        decodeAssertion = new JwtDecodeAssertion();

        decodeAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);

        decodeAssertion.setOutputVariable("tokenOut");
        decodeAssertion.setIncomingToken("token");
        decodeAssertion.setAlgorithmSecretValue("${secret}");

        // Provide a valid token with a JSON payload.
        testPec.setVariable("token", token);
        testPec.setVariable("secret", pemSecret); // What was used to initially sign the token with.

        AssertionStatus status;
        try {
            serverDecodeAssertion = new ServerJwtDecodeAssertion(decodeAssertion, testAppContext);
            status = serverDecodeAssertion.checkRequest(testPec);
            assertEquals("Assertion failed decoding a Base64 Encoded PEM-signed token.", AssertionStatus.NONE, status);
        } catch (PolicyAssertionException e) {
            fail("The test threw an unexpected PolicyAssertionException. (" + e.getMessage() + ")");
        } catch (IOException e) {
            fail("the test threw an unexpected IOException. (" + e.getMessage() + ")");
        }

    }



}
