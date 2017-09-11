package com.l7tech.external.assertions.jwt.server;

import com.l7tech.external.assertions.jwt.CreateJsonWebKeyAssertion;
import com.l7tech.external.assertions.jwt.JwkKeyInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jose4j.jwk.Use;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.l7tech.external.assertions.jwt.JsonWebTokenConstants.PUBLIC_KEY_USE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerCreateJsonWebKeyAssertionTest {

    private static final Goid KEYSTORE_ID = Goid.DEFAULT_GOID;
    private static final String KEY_ALIAS = "alias";
    private static final String EMPTY_JWKS = "{" + System.lineSeparator() +
            "  \"keys\" : [ ]" + System.lineSeparator() +
            "}";

    private TestAudit testAudit;

    @Mock
    private DefaultKey defaultKey;

    private List<String> keyList = null;
    private List<String> publicKeyUses = null;

    @Before
    public void init() throws Exception {
        testAudit = new TestAudit();
        keyList = new ArrayList<>();
        keyList.add("key1");
        keyList.add("key2");
        publicKeyUses = new ArrayList<>();
        publicKeyUses.add(Use.SIGNATURE);
        publicKeyUses.add(Use.ENCRYPTION);
    }

    @Test
    public void test_noKeys_Success() throws Exception {
        setupDefaultMock();
        PolicyEnforcementContext context = getContext();

        CreateJsonWebKeyAssertion ass = new CreateJsonWebKeyAssertion();
        ass.setTargetVariable("result");
        ass.setKeys(Collections.emptyList());

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertEquals(EMPTY_JWKS, jwks);

        assertEquals(0, testAudit.getAuditCount());
    }

    /**
     * Confirms that a JWKS is created for a single specified key, with the expected values.
     *
     * Example JWKS:
     * {
     *      "keys" : [ {
     *          "kty" : "RSA",
     *          "kid" : "key1",
     *          "use" : "sig",
     *          "n" : "kBM6yH792qD18PKOkD5.......",
     *          "e" : "AQAB",
     *          "x5c" : [ "MIICBDCCAW2.......", "MIICBDCCAW2......." ],
     *          "x5t" : "5qbk4Ae4pi+0JNs0eApY01v72sY="
     *      } ]
     * }
     */
    @Test
    public void test_SingleKey_Success() throws Exception {
        setupDefaultMock();
        PolicyEnforcementContext context = getContext();
        JwkKeyInfo key1 = getJwkKeyInfo(keyList.get(0), publicKeyUses.get(0));

        List<JwkKeyInfo> keys = new ArrayList<>();
        keys.add(key1);

        CreateJsonWebKeyAssertion ass = new CreateJsonWebKeyAssertion();
        ass.setTargetVariable("result");
        ass.setKeys(keys);

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertNotNull(jwks);

        JSONData jsonData = JSONFactory.getInstance().newJsonData(jwks);

        assertKeyObject(jsonData, 1);
    }

    /**
     * Confirms that a JWKS is created for the two specified keys, with the expected values.
     *
     * Example JWKS:
     * {
     *      "keys" : [ {
     *          "kty" : "RSA",
     *          "kid" : "key1",
     *          "use" : "sig",
     *          "n" : "kBM6yH792qD18PKOkD5.......",
     *          "e" : "AQAB",
     *          "x5c" : [ "MIICBDCCAW2.......", "MIICBDCCAW2......." ],
     *          "x5t" : "5qbk4Ae4pi+0JNs0eApY01v72sY="
     *      }, {
     *          "kty" : "RSA",
     *          "kid" : "key2",
     *          "use" : "enc",
     *          "n" : "kBM6yH792qD18PKOkD5.......",
     *          "e" : "AQAB",
     *          "x5c" : [ "MIICBDCCAW2.......", "MIICBDCCAW2......." ],
     *          "x5t" : "XD5r14BqrhOc43+iY5ooBCCfmqI="
     *    } ]
     * }
     */
    @Test
    public void test_MultipleKeys_Success() throws Exception {
        setupDefaultMock();
        PolicyEnforcementContext context = getContext();

        JwkKeyInfo key1 = getJwkKeyInfo(keyList.get(0), publicKeyUses.get(0));
        JwkKeyInfo key2 = getJwkKeyInfo(keyList.get(1), publicKeyUses.get(1));

        List<JwkKeyInfo> keys = new ArrayList<>();
        keys.add(key1);
        keys.add(key2);

        CreateJsonWebKeyAssertion ass = new CreateJsonWebKeyAssertion();
        ass.setTargetVariable("result");
        ass.setKeys(keys);

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertNotNull(jwks);

        JSONData jsonData = JSONFactory.getInstance().newJsonData(jwks);
        assertKeyObject(jsonData, 2);
    }

    /**
     *
     * Confirms that a JWKS is created for key specified in context variable.
     *
     *
     * */
    @Test
    public void test_SingleKey_Success_WithContextVar() throws Exception {
        setupDefaultMock();
        PolicyEnforcementContext context = getContext();
        context.setVariable("keyId", keyList.get(0));

        JwkKeyInfo key1 = getJwkKeyInfo("${keyId}", publicKeyUses.get(0));

        List<JwkKeyInfo> keys = new ArrayList<>();
        keys.add(key1);

        CreateJsonWebKeyAssertion ass = new CreateJsonWebKeyAssertion();
        ass.setTargetVariable("result");
        ass.setKeys(keys);

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertNotNull(jwks);

        JSONData jsonData = JSONFactory.getInstance().newJsonData(jwks);
        assertKeyObject(jsonData, 1);
    }

    @Test
    public void test_KeyIdFromNonExistentContextVar_FailWithAudit10840() throws Exception {
        setupDefaultMock();
        PolicyEnforcementContext context = getContext();

        CreateJsonWebKeyAssertion ass = createAssertion("${keyId}", publicKeyUses.get(0));

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertEquals(EMPTY_JWKS, jwks);

        assertEquals(3, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.NO_SUCH_VARIABLE, "keyId"));
        assertTrue(testAudit.isAuditPresentWithParameters(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE, "keyId"));
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.JWT_JWK_ERROR, "Could not find the specified key id"));
    }

    @Test
    public void test_KeyIdNotFound_FailWithAudit10841() throws Exception {
        setupFindExceptionMock();
        PolicyEnforcementContext context = getContext();
        CreateJsonWebKeyAssertion ass = createAssertion("keyId", publicKeyUses.get(0));

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertEquals(EMPTY_JWKS, jwks);

        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.JWT_JWK_NOT_FOUND, KEY_ALIAS));
    }

    @Test
    public void test_KeyStoreNotAccessible_FailWithAudit10803() throws Exception {
        setupKeystoreExceptionMock();
        PolicyEnforcementContext context = getContext();
        CreateJsonWebKeyAssertion ass = createAssertion("keyId", publicKeyUses.get(0));

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertEquals(EMPTY_JWKS, jwks);

        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.JWT_KEYSTORE_ERROR));
    }

    @Test
    public void test_KeyTypeNotSupported_FailWithAudit10805() throws Exception {
        PolicyEnforcementContext context = getContext();
        CreateJsonWebKeyAssertion ass = createAssertion("keyId", publicKeyUses.get(0));

        ServerCreateJsonWebKeyAssertion sass = createServerAssertion(ass);
        AssertionStatus status = sass.checkRequest(context);
        Assert.assertEquals(AssertionStatus.NONE, status);

        String jwks = (String) context.getVariable("result");
        assertEquals(EMPTY_JWKS, jwks);

        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.JWT_JOSE_ERROR, "Unsupported Key Type"));
    }

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @NotNull
    private ServerCreateJsonWebKeyAssertion createServerAssertion(CreateJsonWebKeyAssertion ass) {
        ServerCreateJsonWebKeyAssertion sass = new ServerCreateJsonWebKeyAssertion(ass);

        ApplicationContexts.inject(sass,
                CollectionUtils.<String, Object>mapBuilder()
                        .put("defaultKey", defaultKey)
                        .put("auditFactory", testAudit.factory())
                        .map(),
                false);

        return sass;
    }

    private CreateJsonWebKeyAssertion createAssertion(String key, String publicKeyUse) {
        JwkKeyInfo key1 = getJwkKeyInfo(key, publicKeyUse);

        List<JwkKeyInfo> keys = new ArrayList<>();
        keys.add(key1);

        CreateJsonWebKeyAssertion ass = new CreateJsonWebKeyAssertion();
        ass.setTargetVariable("result");
        ass.setKeys(keys);
        return ass;
    }

    private void setupDefaultMock() throws Exception {
        X509Certificate[] certChain = new X509Certificate[]{new TestCertificateGenerator().subject("CN=test1").generate(), new TestCertificateGenerator().subject("CN=test2").generate()};
        PrivateKey privateKey = new TestCertificateGenerator().getPrivateKey();
        SsgKeyEntry ssgKeyEntry = new SsgKeyEntry(KEYSTORE_ID, KEY_ALIAS, certChain, privateKey);
        when(defaultKey.lookupKeyByKeyAlias(eq(KEY_ALIAS), eq(KEYSTORE_ID))).thenReturn(ssgKeyEntry);
    }

    private void setupKeystoreExceptionMock() throws Exception {
        when(defaultKey.lookupKeyByKeyAlias(any(String.class), any(Goid.class))).thenThrow(new KeyStoreException());
    }

    private void setupFindExceptionMock() throws Exception {
        when(defaultKey.lookupKeyByKeyAlias(any(String.class), any(Goid.class))).thenThrow(new FindException());
    }

    private JwkKeyInfo getJwkKeyInfo(String keyId, String use) {
        JwkKeyInfo key = new JwkKeyInfo();
        key.setSourceKeyGoid(KEYSTORE_ID);
        key.setSourceKeyAlias(KEY_ALIAS);
        key.setKeyId(keyId);
        key.setPublicKeyUse(PUBLIC_KEY_USE.get(use));
        return key;
    }

    private void assertKeyObject(JSONData jsonData, int keyObjectMapSize) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = (Map<String, Object>) jsonData.getJsonObject();
        assertEquals(1, objectMap.size());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> keyObjectMapList = (List<Map<String, String>>) objectMap.get("keys");
        assertEquals(keyObjectMapSize, keyObjectMapList.size());

        for(int i = 0; i < keyObjectMapSize; i++) {
            @SuppressWarnings("unchecked")
            Map<String, String> keyOneObjectMap = keyObjectMapList.get(i);
            assertEquals(7, keyOneObjectMap.size());
            assertEquals("RSA", keyOneObjectMap.get("kty"));
            assertEquals(keyList.get(i), keyOneObjectMap.get("kid"));    // key1 keyId
            assertEquals(publicKeyUses.get(i), keyOneObjectMap.get("use"));  // publicKeyUse "sig"
            assertTrue(keyOneObjectMap.containsKey("n"));
            assertEquals("AQAB", keyOneObjectMap.get("e"));
            assertTrue(keyOneObjectMap.containsKey("x5c"));
            assertTrue(keyOneObjectMap.containsKey("x5t"));

            assertEquals(0, testAudit.getAuditCount());
        }
    }
}
