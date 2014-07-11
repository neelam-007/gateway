package com.l7tech.external.assertions.jsonwebtoken.server;

import com.l7tech.external.assertions.jsonwebtoken.JwtDecodeAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: rseminoff
 * Date: 27/11/12
 */
public class ServerJwtDecodeAssertionTest {
    private String token;

    private JwtDecodeAssertion testAssertion;
    private PolicyEnforcementContext myPec;
    private ServerJwtDecodeAssertion testServer;
    private ApplicationContext appContext;

    @Before
    public void setup() {
        testAssertion = new JwtDecodeAssertion();
        myPec = AssertionTestUtil.getBasicPEC();
        testServer = null;
        token = null;
        appContext = AssertionTestUtil.getTestApplicationContext();
    }

    @Test
    public void testPlainTextJWTFromVariable() {
        token = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.";

        String valueToCompare = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";

        myPec.setVariable("token", token);

        testAssertion.setIncomingToken("${token}");
        testAssertion.setOutputVariable("output");

        AssertionStatus status;
        try {
            testServer = new ServerJwtDecodeAssertion(testAssertion, appContext);
            status = testServer.checkRequest(myPec);
            if (status != AssertionStatus.NONE) {
                fail("Assertion Failed with Status: " + status + ", creating plaintext JWT Token.");
            }

            // Now we're checking output for it's result Value.
            Object output = myPec.getVariable("output");
            if (output instanceof String) {
                // Check the value of the variable.
                assertEquals("Assertion comparison failed", valueToCompare, output);
            } else {
                fail("Assertion returned an unexpected value type");
            }
        } catch (Exception e) {
            fail("Test caused an Exception: " + e.getMessage());
        }
    }

//    @Test
//    public void testValidateTokenWithPEMCertificate() {
//        String certificate = "-----BEGIN CERTIFICATE-----\n" +
//                "MIIDBjCCAe6gAwIBAgIIb0Hq9pdjw7swDQYJKoZIhvcNAQEMBQAwITEfMB0GA1UEAxMWcnNlbWlu\n" +
//                "b2ZmLmw3dGVjaC5sb2NhbDAeFw0xMjExMjExNTUzMjJaFw0yMjExMTkxNTUzMjJaMCExHzAdBgNV\n" +
//                "BAMTFnJzZW1pbm9mZi5sN3RlY2gubG9jYWwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\n" +
//                "AQC9poIWvfX3hoXslboRJZ4ZVM8Ui27zh7wqkR/eZZtk7OBV42Tfxj9o+qFJDtaKLiasY5PxYjZ5\n" +
//                "H0izl4ybvxMHQhUaN9+HLkAKMs3A99S8i4Hf9uWJizY5MJ854fDGD6Vi6ew0Y5hj4V3/e4XjYbhC\n" +
//                "v2X9f/hIZTOJTkk9uZUH4XlfsxRFLDea3j3y8OMHF/qdfna/vGUIwXQ5BYXkn4kg9aVHvWerXO6x\n" +
//                "Xm3aRFBiLxOt4toqbD7DOltgzcxkzeLAqAhOhgX/jg1ZLzzrjVbSowMydSS318Z4VbNGMXhderIZ\n" +
//                "OeB1pesJyymttGk56ObDqsgzh8m9GT04P780DUgHAgMBAAGjQjBAMB0GA1UdDgQWBBTvpgOxlTBH\n" +
//                "9lY2i/15XEkpl34n7DAfBgNVHSMEGDAWgBTvpgOxlTBH9lY2i/15XEkpl34n7DANBgkqhkiG9w0B\n" +
//                "AQwFAAOCAQEAEUSwuglsrJXN984Hwc0r9VqaPADj/K0sfDOwHHNiYvsY6KPWNuahJCl99wb/eECY\n" +
//                "Yc/o8maltEgD/O1pdLrOeqdniXLUDF6ENTlH09ib3EYP8aRoFg3S+2lTNJnjFQkTUq1MFn2HFpDn\n" +
//                "vRYI3RnpgjWvNbVUQLahqYh9XTQ8JFlXQifw6BALpK8+ADjIS3Pf6ft7PyBqIOPhqfMj5Q4gXayb\n" +
//                "sULMKnRtRD9gUbKHdFwnl0GIrDCJr+SR2k4wNUWwH86qgmkx9nnEb8eEfGfGpGHpFiU9nT13iVaM\n" +
//                "W3eWWNo2ii0HuPr7QyKEZ4YHWqzPTU6ddENQMe6UtWcpRTeV5w==\n" +
//                "-----END CERTIFICATE-----";
//        token = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.rdSzpO3v1fpIpmbb01641qNxX4ztABWSlbormLW31x0yv9CVaO45W82GrduOiLJRVv8Hy0Xs9jf1vd2Zwh4WpENiSf2AmQaDkTPwdH7vfRlxfTpUUHG88sFQkNi05so0gLtQQB4sK4glrRslQh4XuIQz_HyNGS30mp57MYBWejlkZ5pPUJJvLBgmzt8ncjDZX3PiDFmbHxXMS9SbLnHpEhuiVax0uJrvzmOhQaeY3WJAM1u3Cu0hJipEMoOItqMHuLd4IVSDf_0hdowIANs8O6_1a59afb3TdNWNOnI9m_6F7bwjxklNg-6OeJzS9JJ81jbgIY0eWYAEQyt3rg_Shg";
//
//        String valueToCompare = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
//
//        myPec.setVariable("token", token);
//        myPec.setVariable("secret", certificate);
//
//        testAssertion.setIncomingToken("${token}");
//        testAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);
//        testAssertion.setAlgorithmSecretValue("${secret}");
//        testAssertion.setOutputVariable("output");
//
//        AssertionStatus status;
//        try {
//            testServer = new ServerJwtDecodeAssertion(testAssertion, appContext);
//            status = testServer.checkRequest(myPec);
//            if (status != AssertionStatus.NONE) {
//                fail("Assertion Failed with Status: " + status + ", decoding supplied JWT Token with PEM certificate");
//            }
//
//            // Now we're checking output for it's result Value.
//            Object output = myPec.getVariable("output");
//            if (output instanceof String) {
//                // Check the value of the variable.
//                assertEquals("Assertion comparison failed", valueToCompare, output);
//            } else {
//                fail("Assertion returned an unexpected value type");
//            }
//        } catch (Exception e) {
//            fail("Test caused an Exception: " + e.getMessage());
//        }
//
//    }
//
//    @Test
//    public void testValidateTokenWithDefinedCertificate() {
//        String certificate = "-----BEGIN CERTIFICATE-----\n" +
//                "MIIDBjCCAe6gAwIBAgIIb0Hq9pdjw7swDQYJKoZIhvcNAQEMBQAwITEfMB0GA1UEAxMWcnNlbWlu\n" +
//                "b2ZmLmw3dGVjaC5sb2NhbDAeFw0xMjExMjExNTUzMjJaFw0yMjExMTkxNTUzMjJaMCExHzAdBgNV\n" +
//                "BAMTFnJzZW1pbm9mZi5sN3RlY2gubG9jYWwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\n" +
//                "AQC9poIWvfX3hoXslboRJZ4ZVM8Ui27zh7wqkR/eZZtk7OBV42Tfxj9o+qFJDtaKLiasY5PxYjZ5\n" +
//                "H0izl4ybvxMHQhUaN9+HLkAKMs3A99S8i4Hf9uWJizY5MJ854fDGD6Vi6ew0Y5hj4V3/e4XjYbhC\n" +
//                "v2X9f/hIZTOJTkk9uZUH4XlfsxRFLDea3j3y8OMHF/qdfna/vGUIwXQ5BYXkn4kg9aVHvWerXO6x\n" +
//                "Xm3aRFBiLxOt4toqbD7DOltgzcxkzeLAqAhOhgX/jg1ZLzzrjVbSowMydSS318Z4VbNGMXhderIZ\n" +
//                "OeB1pesJyymttGk56ObDqsgzh8m9GT04P780DUgHAgMBAAGjQjBAMB0GA1UdDgQWBBTvpgOxlTBH\n" +
//                "9lY2i/15XEkpl34n7DAfBgNVHSMEGDAWgBTvpgOxlTBH9lY2i/15XEkpl34n7DANBgkqhkiG9w0B\n" +
//                "AQwFAAOCAQEAEUSwuglsrJXN984Hwc0r9VqaPADj/K0sfDOwHHNiYvsY6KPWNuahJCl99wb/eECY\n" +
//                "Yc/o8maltEgD/O1pdLrOeqdniXLUDF6ENTlH09ib3EYP8aRoFg3S+2lTNJnjFQkTUq1MFn2HFpDn\n" +
//                "vRYI3RnpgjWvNbVUQLahqYh9XTQ8JFlXQifw6BALpK8+ADjIS3Pf6ft7PyBqIOPhqfMj5Q4gXayb\n" +
//                "sULMKnRtRD9gUbKHdFwnl0GIrDCJr+SR2k4wNUWwH86qgmkx9nnEb8eEfGfGpGHpFiU9nT13iVaM\n" +
//                "W3eWWNo2ii0HuPr7QyKEZ4YHWqzPTU6ddENQMe6UtWcpRTeV5w==\n" +
//                "-----END CERTIFICATE-----";
//
//        token = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.rdSzpO3v1fpIpmbb01641qNxX4ztABWSlbormLW31x0yv9CVaO45W82GrduOiLJRVv8Hy0Xs9jf1vd2Zwh4WpENiSf2AmQaDkTPwdH7vfRlxfTpUUHG88sFQkNi05so0gLtQQB4sK4glrRslQh4XuIQz_HyNGS30mp57MYBWejlkZ5pPUJJvLBgmzt8ncjDZX3PiDFmbHxXMS9SbLnHpEhuiVax0uJrvzmOhQaeY3WJAM1u3Cu0hJipEMoOItqMHuLd4IVSDf_0hdowIANs8O6_1a59afb3TdNWNOnI9m_6F7bwjxklNg-6OeJzS9JJ81jbgIY0eWYAEQyt3rg_Shg";
//
//        CertificateFactory certFactory = null;
//        try {
//            certFactory = CertificateFactory.getInstance("X.509");
//        } catch (CertificateException e) {
//            fail("An exception was thrown creating the X509 instance for this test");
//        }
//        // Consume the PEM certificate in the secret variable.
//        ByteArrayInputStream bais = new ByteArrayInputStream(certificate.getBytes());
//        X509Certificate cert = null;
//        try {
//            cert = (X509Certificate) certFactory.generateCertificate(bais);
//        } catch (CertificateException e) {
//            fail("An exception was thrown generating the certificate from the PEM for this test");
//        }
//
//        try {
//            cert.checkValidity();
//            PublicKey pk = cert.getPublicKey();
//            if (pk == null) {
//                fail("No public key was found with this certificate!");
//            }
//        } catch (CertificateExpiredException e) {
//            fail("An exception was thrown generating the certificate from the PEM for this test (Certificate Expired)");
//        } catch (CertificateNotYetValidException e) {
//            fail("An exception was thrown generating the certificate from the PEM for this test (Certificate Not Yet Valid)");
//        }
//
//        String valueToCompare = "{\"iss\":\"joe\",\r\n \"exp\":1300819380,\r\n \"http://example.com/is_root\":true}";
//
//        myPec.setVariable("token", token);
//        myPec.setVariable("secret", cert);
//
//        testAssertion.setIncomingToken("${token}");
//        testAssertion.setAlgorithmSecretLocation(JwtUtilities.SELECTED_SECRET_VARIABLE);
//        testAssertion.setAlgorithmSecretValue("${secret}");
//        testAssertion.setOutputVariable("output");
//
//        AssertionStatus status;
//        try {
//            testServer = new ServerJwtDecodeAssertion(testAssertion, appContext);
//            status = testServer.checkRequest(myPec);
//            if (status != AssertionStatus.NONE) {
//                fail("Assertion Failed with Status: " + status + ", decoding supplied JWT Token with PEM certificate");
//            }
//
//            // Now we're checking output for it's result Value.
//            Object output = myPec.getVariable("output");
//            if (output instanceof String) {
//                // Check the value of the variable.
//                assertEquals("Assertion comparison failed", valueToCompare, output);
//            } else {
//                fail("Assertion returned an unexpected value type");
//            }
//        } catch (Exception e) {
//            fail("Test caused an Exception: " + e.getMessage());
//        }
//    }
}