package com.l7tech.external.assertions.jwt.server;


import com.l7tech.external.assertions.jwt.EncodeJsonWebTokenAssertion;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import junit.framework.Assert;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.HmacKey;
import org.junit.Test;

public class ServerEncodeJsonWebTokenAssertionTest {
    

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void testJWS_MAC() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("yabbadabbadoo");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey(Keys.MAC_SECRET);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
        Assert.assertEquals("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ", result);

        //verify signature - if this fail, we setup the assertion incorrectly
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(result);
        jws.setKey(new HmacKey(Keys.MAC_SECRET.getBytes("UTF-8")));
        Assert.assertTrue(jws.verifySignature());
    }

    @Test
    public void testJWS_mergeHeaders() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("yabbadabbadoo");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey(Keys.MAC_SECRET);
        ass.setTargetVariable("result");

        ass.setHeaderAction(JsonWebTokenConstants.HEADERS_MERGE);
        //we're going add foo to the headers
        ass.setSourceHeaders("{\"foo\": \"bar\"}");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
        Assert.assertEquals("eyJhbGciOiJIUzI1NiIsImZvbyI6ImJhciJ9.eWFiYmFkYWJiYWRvbw.Ku9INsMxz8gKBvo9CM7vpubNwIhCF1MhfrARh0zml9M", result);

        //verify signature - if this fail, we setup the assertion incorrectly
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(result);
        jws.setKey(new HmacKey(Keys.MAC_SECRET.getBytes("UTF-8")));
        Assert.assertTrue(jws.verifySignature());

        //verify the headers
        Assert.assertEquals("{\"alg\":\"HS256\",\"foo\":\"bar\"}", jws.getHeaders().getFullHeaderAsJsonString());
        Assert.assertEquals(jws.getHeader("alg"), "HS256");
        Assert.assertEquals(jws.getHeader("foo"), "bar");
    }

    @Test
    public void testJWS_replaceHeaders() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("yabbadabbadoo");
        ass.setSignatureAlgorithm("HS256"); //this set the 'alg' to 'HS256'
        ass.setSignatureSecretKey("hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYghJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg");
        ass.setTargetVariable("result");

        ass.setHeaderAction(JsonWebTokenConstants.HEADERS_REPLACE);
        //we're going add foo to the headers
        ass.setSourceHeaders("{\"alg\":\"HS512\",\"foo\":\"bar\"}"); //replace to a diff algo

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
        Assert.assertEquals("eyJhbGciOiJIUzUxMiIsImZvbyI6ImJhciJ9.eWFiYmFkYWJiYWRvbw.NTNeAgdWVw1sAf81FLoXpDqCPSTzmHKDLKF40YaiN1q1ellDdoX6ZHC3hwoPVRjsKeVlDje6nABHILko17bhYA", result);

        //verify signature - if this fail, we setup the assertion incorrectly
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(result);
        jws.setKey(new HmacKey("hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYghJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg".getBytes("UTF-8")));
        Assert.assertTrue(jws.verifySignature());

        //verify the headers
        Assert.assertEquals("{\"alg\":\"HS512\",\"foo\":\"bar\"}", jws.getHeaders().getFullHeaderAsJsonString());
        Assert.assertEquals(jws.getHeader("alg"), "HS512");
        Assert.assertEquals(jws.getHeader("foo"), "bar");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_replaceWithInvalidHeaders() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("yabbadabbadoo");
        ass.setSignatureAlgorithm("HS256"); //this set the 'alg' to 'HS256'
        ass.setSignatureSecretKey("hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYghJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg");
        ass.setTargetVariable("result");

        ass.setHeaderAction(JsonWebTokenConstants.HEADERS_REPLACE);
        //we're going add foo to the headers
        ass.setSourceHeaders("{\"alg\":\"HS512\",\"fo"); //replace to a diff algo

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_missingHeaders() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("yabbadabbadoo");
        ass.setSignatureAlgorithm("HS256"); //this set the 'alg' to 'HS256'
        ass.setSignatureSecretKey("hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYghJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg");
        ass.setTargetVariable("result");

        ass.setHeaderAction(JsonWebTokenConstants.HEADERS_REPLACE);
        //we're going add foo to the headers
        ass.setSourceHeaders("");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test
    public void testJWS_EllipictCurve() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("yabbadabbadoo");
        ass.setSignatureAlgorithm("ES512");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable(Keys.SAMPLE_JWKS);
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setSignatureJwksKeyId("bilbo.baggins@hobbiton.example");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test
    public void testJWS_sourcePayloadFromVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey(Keys.MAC_SECRET);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
        Assert.assertEquals("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ", result);

        //verify signature - if this fail, we setup the assertion incorrectly
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(result);
        jws.setKey(new HmacKey(Keys.MAC_SECRET.getBytes("UTF-8")));
        Assert.assertTrue(jws.verifySignature());
    }

    @Test
    public void testJWS_secretFromVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("my.secret", Keys.MAC_SECRET);
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey("${my.secret}");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
        Assert.assertEquals("eyJhbGciOiJIUzI1NiJ9.dGVzdHBheWxvYWQ.ufyFzikO0U7yG2m3ssFzVrXXzZPLsB4QyJByqaFJ1rc", result);

        //verify signature - if this fail, we setup the assertion incorrectly
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(result);
        jws.setKey(new HmacKey(Keys.MAC_SECRET.getBytes("UTF-8")));
        Assert.assertTrue(jws.verifySignature());
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_emptySourcePayload() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey("");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_emptySourcePayloadFromVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("empty", "");
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${empty}");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey("");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_secretFromEmptyVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("my.secret", "");
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey("${my.secret}");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_secretEmpty() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey("");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test
    public void testJWS_usingJWK() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable(Keys.SAMPLE_JWK_RSA);
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test
    public void testJWS_usingJWKFromContextVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("jwk", Keys.SAMPLE_JWK_RSA);
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable("${jwk}");
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_usingInvalidJWK() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("jwk", "not a json jwk object");
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable("${jwk}");
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_noKeyFound() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("jwk", "");
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable("${jwk}");
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test
    public void testJWS_usingJWKS() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable(Keys.SAMPLE_JWKS);
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setSignatureJwksKeyId("bilbo.baggins@hobbiton.example");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test
    public void testJWS_usingJWKSFromContextVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("jwks", Keys.SAMPLE_JWKS);
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable("${jwks}");
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setSignatureJwksKeyId("bilbo.baggins@hobbiton.example");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_usingJWKSWithNonExistentKeyId() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable(Keys.SAMPLE_JWKS);
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setSignatureJwksKeyId("i.dont.exist");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_usingJWKSWithNoKeyIdSpecified() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable(Keys.SAMPLE_JWKS);
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setSignatureJwksKeyId("");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWS_usingInvalidJWKS() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("testpayload");
        ass.setSignatureAlgorithm("RS256");
        ass.setPrivateKeyFromVariable(true);
        ass.setSignatureSourceVariable("booohoohooo");
        ass.setSignatureKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setSignatureJwksKeyId("i.dont.exist");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    //section below is testing encryption
    @Test
    public void testJWE_fromDER() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");

        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey(Keys.SAMPLE_CERTIFICATE);
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_CERTIFICATE);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        System.out.println(result);
        Assert.assertNotNull(result);
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWE_fromInvalidDER() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");

        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey("lkajdlkgjadsfadfadfadf");
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_CERTIFICATE);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test
    public void testJWE_fromDERInContextVariable() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");
        context.setVariable("super.duper", Keys.SAMPLE_CERTIFICATE);
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey("${super.duper}");
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_CERTIFICATE);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test
    public void testJWE_fromJWK() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");

        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey(Keys.SAMPLE_JWK_RSA);
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_JWK);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        System.out.println(result);
        Assert.assertNotNull(result);
    }

    @Test
    public void testJWE_fromJWKS() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");

        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey(Keys.SAMPLE_JWKS);
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setEncryptionKeyId("bilbo.baggins@hobbiton.example");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test
    public void testJWE_fromJWKSDirect() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");

        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm(KeyManagementAlgorithmIdentifiers.DIRECT);
        ass.setContentEncryptionAlgorithm(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        ass.setEncryptionKey(Keys.SAMPLE_JWKS);
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setEncryptionKeyId("18ec08e1-bfa9-4d95-b205-2b4dd1d4321d");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWE_noEncryptionKeySpecified() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");

        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey("");
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test(expected = NoSuchVariableException.class)
    public void testJWE_noJWKSKeyIdSpecified() throws Exception {
        PolicyEnforcementContext context = getContext();
        context.setVariable("testpayload", "yabbadabbadoo");

        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("${testpayload}");
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey("asdfasdf");
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setEncryptionKeyId("");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.FAILED, status);
        context.getVariable("result.compact");
    }

    @Test
    public void test_jwsAndJwe() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();

        ass.setSourceVariable("yabbadabbadoo");

        //jws setup
        ass.setSignatureAlgorithm("HS256");
        ass.setSignatureSecretKey(Keys.MAC_SECRET);
        ass.setTargetVariable("result");

        //jwe setup
        ass.setKeyManagementAlgorithm("RSA1_5");
        ass.setContentEncryptionAlgorithm("A128CBC-HS256");
        ass.setEncryptionKey(Keys.SAMPLE_JWKS);
        ass.setEncryptionKeyType(JsonWebTokenConstants.KEY_TYPE_JWKS);
        ass.setEncryptionKeyId("bilbo.baggins@hobbiton.example");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);

        //create key
        RsaJsonWebKey jwk = (RsaJsonWebKey) JsonWebKey.Factory.newJwk("       {\n" +
                "         \"kty\": \"RSA\",\n" +
                "         \"kid\": \"bilbo.baggins@hobbiton.example\",\n" +
                "         \"use\": \"enc\",\n" +
                "         \"n\": \"n4EPtAOCc9AlkeQHPzHStgAbgs7bTZLwUBZdR8_KuKPEHLd4rHVTeT\n" +
                "             -O-XV2jRojdNhxJWTDvNd7nqQ0VEiZQHz_AJmSCpMaJMRBSFKrKb2wqV\n" +
                "             wGU_NsYOYL-QtiWN2lbzcEe6XC0dApr5ydQLrHqkHHig3RBordaZ6Aj-\n" +
                "             oBHqFEHYpPe7Tpe-OfVfHd1E6cS6M1FZcD1NNLYD5lFHpPI9bTwJlsde\n" +
                "             3uhGqC0ZCuEHg8lhzwOHrtIQbS0FVbb9k3-tVTU4fg_3L_vniUFAKwuC\n" +
                "             LqKnS2BYwdq_mzSnbLY7h_qixoR7jig3__kRhuaxwUkRz5iaiQkqgc5g\n" +
                "             HdrNP5zw\",\n" +
                "         \"e\": \"AQAB\",\n" +
                "         \"d\": \"bWUC9B-EFRIo8kpGfh0ZuyGPvMNKvYWNtB_ikiH9k20eT-O1q_I78e\n" +
                "             iZkpXxXQ0UTEs2LsNRS-8uJbvQ-A1irkwMSMkK1J3XTGgdrhCku9gRld\n" +
                "             Y7sNA_AKZGh-Q661_42rINLRCe8W-nZ34ui_qOfkLnK9QWDDqpaIsA-b\n" +
                "             MwWWSDFu2MUBYwkHTMEzLYGqOe04noqeq1hExBTHBOBdkMXiuFhUq1BU\n" +
                "             6l-DqEiWxqg82sXt2h-LMnT3046AOYJoRioz75tSUQfGCshWTBnP5uDj\n" +
                "             d18kKhyv07lhfSJdrPdM5Plyl21hsFf4L_mHCuoFau7gdsPfHPxxjVOc\n" +
                "             OpBrQzwQ\",\n" +
                "         \"p\": \"3Slxg_DwTXJcb6095RoXygQCAZ5RnAvZlno1yhHtnUex_fp7AZ_9nR\n" +
                "             aO7HX_-SFfGQeutao2TDjDAWU4Vupk8rw9JR0AzZ0N2fvuIAmr_WCsmG\n" +
                "             peNqQnev1T7IyEsnh8UMt-n5CafhkikzhEsrmndH6LxOrvRJlsPp6Zv8\n" +
                "             bUq0k\",\n" +
                "         \"q\": \"uKE2dh-cTf6ERF4k4e_jy78GfPYUIaUyoSSJuBzp3Cubk3OCqs6grT\n" +
                "             8bR_cu0Dm1MZwWmtdqDyI95HrUeq3MP15vMMON8lHTeZu2lmKvwqW7an\n" +
                "             V5UzhM1iZ7z4yMkuUwFWoBvyY898EXvRD-hdqRxHlSqAZ192zB3pVFJ0\n" +
                "             s7pFc\",\n" +
                "         \"dp\": \"B8PVvXkvJrj2L-GYQ7v3y9r6Kw5g9SahXBwsWUzp19TVlgI-YV85q\n" +
                "             1NIb1rxQtD-IsXXR3-TanevuRPRt5OBOdiMGQp8pbt26gljYfKU_E9xn\n" +
                "             -RULHz0-ed9E9gXLKD4VGngpz-PfQ_q29pk5xWHoJp009Qf1HvChixRX\n" +
                "             59ehik\",\n" +
                "         \"dq\": \"CLDmDGduhylc9o7r84rEUVn7pzQ6PF83Y-iBZx5NT-TpnOZKF1pEr\n" +
                "             AMVeKzFEl41DlHHqqBLSM0W1sOFbwTxYWZDm6sI6og5iTbwQGIC3gnJK\n" +
                "             bi_7k_vJgGHwHxgPaX2PnvP-zyEkDERuf-ry4c_Z11Cq9AqC2yeL6kdK\n" +
                "             T1cYF8\",\n" +
                "         \"qi\": \"3PiqvXQN0zwMeE-sBvZgi289XP9XCQF3VWqPzMKnIgQp7_Tugo6-N\n" +
                "             ZBKCQsMf3HaEGBjTVJs_jcK8-TRXvaKe-7ZMaQj8VfBdYkssbu0NKDDh\n" +
                "             jJ-GtiseaDVWt7dcH0cfwxgFUHpQh7FoCrjFJ6h6ZEpMF6xmujs4qMpP\n" +
                "             z8aaI4\"\n" +
                "       }");

        //verify encryption
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setCompactSerialization(result);
        jwe.setKey(jwk.getRsaPrivateKey());

        Assert.assertEquals(jwe.getHeader("cty"), "JWT");
        Assert.assertEquals("eyJhbGciOiJIUzI1NiJ9.eWFiYmFkYWJiYWRvbw.widHhV78UyVWXA_hzwY88B0p6jMOS2vZMntVT6xxFHQ", jwe.getPlaintextString());

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(jwe.getPlaintextString());
        jws.setKey(new HmacKey(Keys.MAC_SECRET.getBytes()));

        Assert.assertTrue(jws.verifySignature());

    }

    @Test
    public void test_unsecure() throws Exception {
        PolicyEnforcementContext context = getContext();
        EncodeJsonWebTokenAssertion ass = new EncodeJsonWebTokenAssertion();
        ass.setSourceVariable("yabbadabbadoo");
        ass.setSignatureAlgorithm("None");
        ass.setKeyManagementAlgorithm("None");
        ass.setTargetVariable("result");

        ServerEncodeJsonWebTokenAssertion sass = new ServerEncodeJsonWebTokenAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);
        final String result = (String) context.getVariable("result.compact");
        Assert.assertNotNull(result);
        Assert.assertEquals("eyJhbGciOiJub25lIn0.eWFiYmFkYWJiYWRvbw.", result);

    }
}
