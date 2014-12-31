package com.l7tech.external.assertions.jwt.server;

import com.google.common.collect.Lists;
import com.l7tech.external.assertions.jwt.DecodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import junit.framework.Assert;
import org.junit.Test;


public class ServerDecodeJsonWebTokenAssertionTest {

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void test_invalidType() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("this.does.not.work.it.has.too.many.parts");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_missingPayload() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_missingPayloadFromVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("foo", "");
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("${foo}");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_JWS_noValidation() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_NONE);
        assertion.setTargetVariablePrefix("result");
        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWS", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"HS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("HS256", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg"), context.getVariable("result.header.names"));
        Assert.assertEquals("yabbadabbadoo", context.getVariable("result.payload"));
        Assert.assertEquals("widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ", context.getVariable("result.signature"));
    }

    @Test(expected = NoSuchVariableException.class)
         public void test_JWS_noValidationInvalidHeader00() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        //invalid base64 encoding of header
        assertion.setSourcePayload("eyJhbGciOiJIUzI1NiJ8.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_NONE);
        assertion.setTargetVariablePrefix("result");
        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);

        context.getVariable("result.header");
    }

    @Test(expected = NoSuchVariableException.class)
    public void test_JWS_noValidationInvalidHeader01() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        //invalid json header
        assertion.setSourcePayload("eyJhbGciOiJIUzI1NiI.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_NONE);
        assertion.setTargetVariablePrefix("result");
        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);

        context.getVariable("result.header");
    }


    @Test
    public void test_JWE_noValidation() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.TRV-apcyms-COXBNuPEbuHx9fI6T2oLV7jha67xvAc1UeJZWw3lFBdEh0raHyfqie4fcCOY-qbY5GlEp7U9H5YIqdiRn1wmvEQc62BASSEVEmC_MFCtgyO4bFIhZQENy6XjEDGQh0ukrAhDdqKNo-7Sw98t4Bc1Q6ZRK2ClSD0rGbqqH5I8yYbP-1sexvRHEAQRpVilK2_vKIiDIF5XhxZKNIXRFgQHYHUcmQ6PDik0F22IU7RDlS_3peu4d4JoNYUuQYZv7PxJLkSE0nskhlmlufS_qfgpaowQtH4ES0bDh8HsKx9qPXzKHIElkbv1aaFmoXQzppKwrVzZq4AaZwQ.1e2XIdvYKRqVAb5iEYroPg.VYet13NQ3nCWscICwskDig.YJVWxf6nJGG4Q-ls64kzGQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_NONE);
        assertion.setTargetVariablePrefix("result");
        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWE", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"RSA1_5\",\"enc\":\"A128CBC-HS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("RSA1_5", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg", "enc"), context.getVariable("result.header.names"));
        Assert.assertEquals("TRV-apcyms-COXBNuPEbuHx9fI6T2oLV7jha67xvAc1UeJZWw3lFBdEh0raHyfqie4fcCOY-qbY5GlEp7U9H5YIqdiRn1wmvEQc62BASSEVEmC_MFCtgyO4bFIhZQENy6XjEDGQh0ukrAhDdqKNo-7Sw98t4Bc1Q6ZRK2ClSD0rGbqqH5I8yYbP-1sexvRHEAQRpVilK2_vKIiDIF5XhxZKNIXRFgQHYHUcmQ6PDik0F22IU7RDlS_3peu4d4JoNYUuQYZv7PxJLkSE0nskhlmlufS_qfgpaowQtH4ES0bDh8HsKx9qPXzKHIElkbv1aaFmoXQzppKwrVzZq4AaZwQ", context.getVariable("result.encrypted_key"));
        Assert.assertEquals("1e2XIdvYKRqVAb5iEYroPg", context.getVariable("result.initialization_vector"));
        Assert.assertEquals("VYet13NQ3nCWscICwskDig", context.getVariable("result.cipher_text"));
        Assert.assertEquals("YJVWxf6nJGG4Q-ls64kzGQ", context.getVariable("result.authentication_tag"));
    }

    @Test
    public void test_verifyValidJWS() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_SECRET);
        assertion.setSignatureSecret(Keys.MAC_SECRET);
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWS", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"HS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("HS256", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg"), context.getVariable("result.header.names"));
        Assert.assertEquals("yabbadabbadoo", context.getVariable("result.payload"));
        Assert.assertEquals("widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ", context.getVariable("result.signature"));
        Assert.assertEquals("true", context.getVariable("result.valid"));
    }

    @Test
    public void test_verifyInvalidJWS() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_SECRET);
        assertion.setSignatureSecret("hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYh");
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWS", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"HS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("HS256", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg"), context.getVariable("result.header.names"));
        Assert.assertEquals("yabbadabbadoo", context.getVariable("result.payload"));
        Assert.assertEquals("widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ", context.getVariable("result.signature"));
        Assert.assertEquals("false", context.getVariable("result.valid"));
    }

    @Test
    public void test_verifyInvalidJWSHeader() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_SECRET);
        assertion.setSignatureSecret("hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYh");
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_verifyJWS_noSecret() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_SECRET);
        assertion.setSignatureSecret("");
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_verifyJWS_noSecretFromVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("secret", "");
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_SECRET);
        assertion.setSignatureSecret("${secret}");
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_verifyJWS_invalidType() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.TRV-apcyms-COXBNuPEbuHx9fI6T2oLV7jha67xvAc1UeJZWw3lFBdEh0raHyfqie4fcCOY-qbY5GlEp7U9H5YIqdiRn1wmvEQc62BASSEVEmC_MFCtgyO4bFIhZQENy6XjEDGQh0ukrAhDdqKNo-7Sw98t4Bc1Q6ZRK2ClSD0rGbqqH5I8yYbP-1sexvRHEAQRpVilK2_vKIiDIF5XhxZKNIXRFgQHYHUcmQ6PDik0F22IU7RDlS_3peu4d4JoNYUuQYZv7PxJLkSE0nskhlmlufS_qfgpaowQtH4ES0bDh8HsKx9qPXzKHIElkbv1aaFmoXQzppKwrVzZq4AaZwQ.1e2XIdvYKRqVAb5iEYroPg.VYet13NQ3nCWscICwskDig.YJVWxf6nJGG4Q-ls64kzGQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_SECRET);
        assertion.setTargetVariablePrefix("result");
        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_verifyValidJWS_JWKS() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJFUzUxMiJ9.eWFiYmFkYWJiYWRvbw.AJGzMW3Oz8SeOHyjmPt72PT9Hen6IwAsnJww1Wwp631AE_M-u1rJgk9I4XocwK2hlgzbfVsB1G4pGl9t7Tz5GjRzAYGbUwewTDo4EI2P6uvJG13KVUyYSEtJ0rQtLLRWPdUc8pAavuDr7PxHc4PedbgdlCA_fdfeRwv1kbkKB4xN6jXI");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_CV);
        assertion.setKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        assertion.setKeyId("bilbo.baggins@hobbiton.example");
        assertion.setPrivateKeySource(Keys.SAMPLE_JWKS);
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWS", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"ES512\"}", context.getVariable("result.header"));
        Assert.assertEquals("ES512", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg"), context.getVariable("result.header.names"));
        Assert.assertEquals("yabbadabbadoo", context.getVariable("result.payload"));
        Assert.assertEquals("AJGzMW3Oz8SeOHyjmPt72PT9Hen6IwAsnJww1Wwp631AE_M-u1rJgk9I4XocwK2hlgzbfVsB1G4pGl9t7Tz5GjRzAYGbUwewTDo4EI2P6uvJG13KVUyYSEtJ0rQtLLRWPdUc8pAavuDr7PxHc4PedbgdlCA_fdfeRwv1kbkKB4xN6jXI", context.getVariable("result.signature"));
        Assert.assertEquals("true", context.getVariable("result.valid"));
    }

    @Test
    public void test_verifyValidJWS_JWK() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJSUzI1NiJ9.dGVzdHBheWxvYWQ.k2pjWhlBWGi8sc0a8UhIs7bl1Z8dUylrAVU3jKrzR0TPmOtlPOsRXjSX-iIe6fXo58KI3W_dx8aC9_4y5oYtL21drQXOTwjSssNELxU3OHe3QvWXXDGD_FNaOXlQswDpvuYrbvW_YppL6iDo6jrMlOvl-hkM6_EoImESNvzJhGeCIUBb53WjqXEer2PKNAiNLcvYi-uUOfg1i6jR-fNCrnyP-fWLgQq5TuazZBvA1PH-LkYzS31TmPQ4ratIM5iz1nN6bQYrAT_PbAgI87liJUF6FWgGDotz7oKDxV_f8RCWheFImeo5FqrErPw_HUXs6Mo3z_prslJ4J8sL5_TPQQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_CV);
        assertion.setKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        assertion.setPrivateKeySource(Keys.SAMPLE_JWK_RSA_SIG);
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWS", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"RS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("RS256", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg"), context.getVariable("result.header.names"));
        Assert.assertEquals("testpayload", context.getVariable("result.payload"));
        Assert.assertEquals("k2pjWhlBWGi8sc0a8UhIs7bl1Z8dUylrAVU3jKrzR0TPmOtlPOsRXjSX-iIe6fXo58KI3W_dx8aC9_4y5oYtL21drQXOTwjSssNELxU3OHe3QvWXXDGD_FNaOXlQswDpvuYrbvW_YppL6iDo6jrMlOvl-hkM6_EoImESNvzJhGeCIUBb53WjqXEer2PKNAiNLcvYi-uUOfg1i6jR-fNCrnyP-fWLgQq5TuazZBvA1PH-LkYzS31TmPQ4ratIM5iz1nN6bQYrAT_PbAgI87liJUF6FWgGDotz7oKDxV_f8RCWheFImeo5FqrErPw_HUXs6Mo3z_prslJ4J8sL5_TPQQ", context.getVariable("result.signature"));
        Assert.assertEquals("true", context.getVariable("result.valid"));
    }

    @Test
    public void test_verifyValidJWE_JWK() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.eB2jgoPG3hopqFWE2jy1qXjQEgMB5uMbSZr7wCo3u2yRLxHhWKEOSbHFdEB__lys3TOmBrl0edincgEf1AGjY4TqOCIoH7FbQeD37iG7F9bc5Gsdl9kdj1sgBO7oxd3ihTGYA8RELD03BZc96iGJZ4ajiAjTIhGCi9-k19Rv5x0EvhAb2ph66QVu_P5VGla04SY28YtEpP6g6k2jhby4Yy9Sduspnnsb8k37VuTZQUzvZTJHv8DOKbJ1UPOvpRywD7K4EQ5IenbyyHz3huqSXulTEL1h-4qaISbOwT8R9yHAsNu352j-3BEKrzaVBDtjdh-HeRTHCYOhz1auzufbiQ.Rjv8ZOQcMwv-_a4QSgUxnA.idXDoOekSkvdl4DzNATXEA.vdK5-ymmUCuaI73zakmt7w");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_CV);
        assertion.setKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        assertion.setPrivateKeySource(Keys.SAMPLE_JWK_RSA_SIG);
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWE", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"RSA1_5\",\"enc\":\"A128CBC-HS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("RSA1_5", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg", "enc"), context.getVariable("result.header.names"));
        Assert.assertEquals("yabbadabbadoo", context.getVariable("result.plaintext"));
        Assert.assertEquals("true", context.getVariable("result.valid"));
    }

    @Test
    public void test_verifyValidJWE_JWKS() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.bUlFvbJw-AiEDNqH6aBvONsnNl2iNFgxGZ3aU7hm6W6P-F9-i_MJbRXdkIPg2fnTbcqmW3uYutdhRdtvzjlH-X44l5Fr_PXTOZPtwSZZxwHxhUE-_0HdibnM13UzFGEJcs3U58dbwYlbYjiNuSO-5JwoT65KCFZdFB1mCM3qoY55fqTKk2WNEWRqD-P-5g4MhzdzJdoQSs9We8wEV-QtKS_RlzFVI7BGmmD3ElZ9OKUN6tz8HVVYDHl2H6wHzyvd4YiX5PYRnio_L69GAEv73fQJel_kpTmge22lE3EDOsLqh7cmS6KuwbvgWzRPa3TMRQ98ieXBURAQ21QyTboBEg.MJjkHqEFZzpamWZ_UF_Eow.ifzXVYBK43soy8vJKv4XKA.BDGG-j0At26LZau4EUeDxg");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_CV);
        assertion.setKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        assertion.setPrivateKeySource(Keys.SAMPLE_JWKS);
        assertion.setTargetVariablePrefix("result");
        assertion.setKeyId("bilbo.baggins@hobbiton.example");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWE", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"RSA1_5\",\"enc\":\"A128CBC-HS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("RSA1_5", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg", "enc"), context.getVariable("result.header.names"));
        Assert.assertEquals("yabbadabbadoo", context.getVariable("result.plaintext"));
        Assert.assertEquals("true", context.getVariable("result.valid"));
    }


    @Test
    public void test_unsecure() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJhbGciOiJub25lIn0.eWFiYmFkYWJiYWRvbw.");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_NONE);

        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWS", context.getVariable("result.type"));
        Assert.assertEquals("{\"alg\":\"none\"}", context.getVariable("result.header"));
        Assert.assertEquals("none", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("alg"), context.getVariable("result.header.names"));
        Assert.assertEquals("yabbadabbadoo", context.getVariable("result.payload"));

    }

    @Test
    public void test_jws() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.Zm9vYmFy.T6knOzWwgtHuCzmSwq5KzWlzmUo_Enl0kyjEe-naPwyPftBlkM1LY08OVZvtAjt1QiywkYIYJUcXv4KQSBQ-tFlv7AyvYrjt1sPfI19Cv2A1f39dWgiEK8N-wsPajQ916JmY15ZOzXzlXhgqQuHoIoh-ok86Xc5qmxnJ6W4TZDl9Yr0vNS-Dxcho4icd-9K31FaUXgErtdgT937FZjKjjGo8rVbUHBtil8lurT6KA1NG6cinF27qtDOleTUxdK3r_qZgrc8Mdi-uxaKd7ZfwiEU1UNzmSZ7XcKChRgxRr8EROrIEIgfRhHL4fjKcvqd4UcuLpKqsJbKTaorC7YsDGQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_CV);
        assertion.setKeyType(JsonWebTokenConstants.KEY_TYPE_CERTIFICATE);
        assertion.setPrivateKeySource(Keys.SAMPLE_VERIFY_CERT);
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        Assert.assertEquals("JWS", context.getVariable("result.type"));
        Assert.assertEquals("{\"typ\":\"JWT\",\"alg\":\"RS256\"}", context.getVariable("result.header"));
        Assert.assertEquals("RS256", context.getVariable("result.header.alg"));
        Assert.assertEquals(Lists.newArrayList("typ", "alg"), context.getVariable("result.header.names"));
        Assert.assertEquals("foobar", context.getVariable("result.payload"));
        Assert.assertEquals("T6knOzWwgtHuCzmSwq5KzWlzmUo_Enl0kyjEe-naPwyPftBlkM1LY08OVZvtAjt1QiywkYIYJUcXv4KQSBQ-tFlv7AyvYrjt1sPfI19Cv2A1f39dWgiEK8N-wsPajQ916JmY15ZOzXzlXhgqQuHoIoh-ok86Xc5qmxnJ6W4TZDl9Yr0vNS-Dxcho4icd-9K31FaUXgErtdgT937FZjKjjGo8rVbUHBtil8lurT6KA1NG6cinF27qtDOleTUxdK3r_qZgrc8Mdi-uxaKd7ZfwiEU1UNzmSZ7XcKChRgxRr8EROrIEIgfRhHL4fjKcvqd4UcuLpKqsJbKTaorC7YsDGQ",
                context.getVariable("result.signature"));

    }

    @Test
    public void test_verifyValidJWE_invalidKeyUsage() throws Exception {
        PolicyEnforcementContext context = getContext();
        DecodeJsonWebTokenAssertion assertion = new DecodeJsonWebTokenAssertion();
        assertion.setSourcePayload("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.Zm9vYmFy.T6knOzWwgtHuCzmSwq5KzWlzmUo_Enl0kyjEe-naPwyPftBlkM1LY08OVZvtAjt1QiywkYIYJUcXv4KQSBQ-tFlv7AyvYrjt1sPfI19Cv2A1f39dWgiEK8N-wsPajQ916JmY15ZOzXzlXhgqQuHoIoh-ok86Xc5qmxnJ6W4TZDl9Yr0vNS-Dxcho4icd-9K31FaUXgErtdgT937FZjKjjGo8rVbUHBtil8lurT6KA1NG6cinF27qtDOleTUxdK3r_qZgrc8Mdi-uxaKd7ZfwiEU1UNzmSZ7XcKChRgxRr8EROrIEIgfRhHL4fjKcvqd4UcuLpKqsJbKTaorC7YsDGQ");
        assertion.setValidationType(JsonWebTokenConstants.VALIDATION_USING_CV);
        assertion.setKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        assertion.setPrivateKeySource(Keys.SAMPLE_JWK_RSA_ENC);
        assertion.setTargetVariablePrefix("result");

        ServerDecodeJsonWebTokenAssertion sass = new ServerDecodeJsonWebTokenAssertion(assertion);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
    }
}
