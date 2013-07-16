package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManageApiKeyAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApiKeyDataResourceHandler;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApiKeyResource;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApiKeyResourceHandler;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.text.MessageFormat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Test the ApiPortalIntegrationAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerManageApiKeyAssertionTest {
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private PortalGenericEntityManager<ApiKeyData> apiKeyManager;
    @Mock
    private ApiKeyResourceHandler keyResourceHandler;
    @Mock
    private ApiKeyDataResourceHandler keyLegacyResourceHandler;

    private PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

    private ManageApiKeyAssertion ass = new ManageApiKeyAssertion();
    
    @Before
    public void setUp() {
        
    }
    
    final static String KEY_RECORD =
            "<l7:ApiKey status=\"active\" xmlns:l7=\"http://ns.l7tech.com/2011/08/portal-api-keys\">\n" +
            "  <l7:Value>vctest-key-{0}</l7:Value><l7:Service>{1}</l7:Service><l7:Plan>{2}</l7:Plan>\n" +
            "  <l7:Secret>{3}</l7:Secret>\n" +
            "  <l7:OrgId>l7org</l7:OrgId>\n" +
            "</l7:ApiKey>";

    public String generateTestData() {

        final int keyRecordsCount = 4000;
        final String serviceOid = "229378";
        final String plan = "lrs-136";
        final String secret = "df1a523baebf4452b8aef5b14d9d7ccd";

        StringBuffer sb = new StringBuffer();

        for (int i=0; i<keyRecordsCount; i++) {
            sb.append( MessageFormat.format(KEY_RECORD, Integer.toString(4001+i), serviceOid, plan, secret ) ).append("\n");
        }

        return sb.toString();
    }
    
    final static String TEST_RECORD_XML =
            "<l7:ApiKey enabled=\"true\" status=\"active\" xmlns:l7=\"http://ns.l7tech.com/2011/08/portal-api-keys\">\n" +
            "  <l7:Value>mykey</l7:Value>\n" +
            "  <l7:Services>\n" +
            "    <l7:S id=\"12345\" plan=\"default\" />\n" +
            "  </l7:Services>\n" +
            "  <l7:Secret>shhh</l7:Secret>\n" +
            "  <l7:OrgId>l7org</l7:OrgId>\n" +
            "</l7:ApiKey>";

    private void assertNoSuchVariable(PolicyEnforcementContext context, String var) {
        try {
            context.getVariable(var);
            fail("Context contained variable " + var + " when it should not have");
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }

    @Test
    public void testAddKey() throws Exception {
        ass.setAction("Add");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        verify(apiKeyManager, never()).add(any(ApiKeyData.class));
        assertEquals("mykey", context.getVariable("prefix.key"));
    }

    @Test
    public void testAddKey_missingKey() throws Exception {
        ass.setAction("Add");
        ass.setApiKey(null);
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, result);
        assertNoSuchVariable(context, "prefix.key");
    }

    @Test
    public void testAddKey_emptyKey() throws Exception {
        ass.setAction("Add");
        ass.setApiKey("");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, result);
        assertNoSuchVariable(context, "prefix.key");
    }

    @Test
    public void testAddKey_missingXml() throws Exception {
        ass.setAction("Add");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(null);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, result);
        assertNoSuchVariable(context, "prefix.key");
    }

    @Test
    public void testAddKey_emptyXml() throws Exception {
        ass.setAction("Add");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement("");
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, result);
        assertNoSuchVariable(context, "prefix.key");
    }

    @Test
    public void testAddKey_dupe() throws Exception {
        when(keyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        when(keyLegacyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        
        ass.setAction("Add");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.FALSIFIED, result);
        verify(apiKeyManager, never()).add(any(ApiKeyData.class));
        assertNoSuchVariable(context, "prefix.key");
    }

    @Test
    public void testDeleteKey() throws Exception {
        ass.setAction("Remove");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyResourceHandler).delete(anyString());
    }

    @Test
    public void testDeleteLegacyKey() throws Exception {
        ass.setAction("Remove");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyLegacyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).delete(anyString());
    }

    @Test
    public void testUpdateKey() throws Exception {
        ass.setAction("Update");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyResourceHandler).put(any(ApiKeyResource.class));
    }

    @Test
     public void testUpdateLegacyKey() throws Exception {
        ass.setAction("Update");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyLegacyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        when(keyResourceHandler.get(anyString())).thenReturn(null).thenReturn(new ApiKeyResource());
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler, times(2)).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyResourceHandler, times(2)).put(any(ApiKeyResource.class));
        verify(keyLegacyResourceHandler).delete(anyString());
    }

    @Test
    public void testUpdateLegacyKeyButWithException() throws Exception {
        ass.setAction("Update");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyLegacyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        when(keyResourceHandler.get(anyString())).thenReturn(null).thenReturn(null);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);
        verify(keyResourceHandler, times(2)).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyResourceHandler).put(any(ApiKeyResource.class));
        verify(keyLegacyResourceHandler).delete(anyString());
    }

    @Test
    public void testUpdateKey_AssignApis() throws Exception {
        ass.setAction("Update");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        context.setVariable("pman.options.assignApis","apiId;planId");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyResourceHandler).put(any(ApiKeyResource.class));
    }

    @Test
    public void testUpdateKey_KeyStatus() throws Exception {
        ass.setAction("Update");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        context.setVariable("pman.options.apikeyStatus","suspend");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyResourceHandler).put(any(ApiKeyResource.class));
    }

    @Test
    public void testUpdateKey_KeySecret() throws Exception {
        ass.setAction("Update");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyResourceHandler.get(anyString())).thenReturn(new ApiKeyResource());
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        context.setVariable("pman.options.apikeySecret","newSecretKey");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyResourceHandler).put(any(ApiKeyResource.class));
    }

    @Test
    public void testUpdateKey_norecords() throws Exception {
        ass.setAction("Update");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
    }

    @Test
     public void testRemoveKey_norecords() throws Exception {
        ass.setAction("Remove");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyResourceHandler.get(anyString())).thenReturn(null);
        when(keyLegacyResourceHandler.get(anyString())).thenReturn(null);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
    }

    @Test
    public void testInvalidAction() throws Exception {
        ass.setAction("duh");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyResourceHandler.get(anyString())).thenReturn(null);
        when(keyLegacyResourceHandler.get(anyString())).thenReturn(null);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        verify(keyResourceHandler).get(anyString());
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
    }

    @Test
    public void testWithException() throws Exception {
        ass.setAction("duh");
        ass.setApiKey("mykey");
        ass.setVariablePrefix("prefix");
        ass.setApiKeyElement(TEST_RECORD_XML);
        when(keyLegacyResourceHandler.get(anyString())).thenThrow(new FindException("Dummy FindException"));
        when(keyResourceHandler.get(anyString())).thenReturn(null);
        ServerManageApiKeyAssertion sass = new ServerManageApiKeyAssertion(ass, applicationContext, apiKeyManager, keyResourceHandler, keyLegacyResourceHandler);

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, result);
        verify(keyLegacyResourceHandler).get(anyString());
        verify(keyResourceHandler, never()).get(anyString());
        verify(keyResourceHandler, never()).put(any(ApiKeyResource.class));
        verify(keyLegacyResourceHandler, never()).put(any(ApiKeyResource.class));
    }


}
