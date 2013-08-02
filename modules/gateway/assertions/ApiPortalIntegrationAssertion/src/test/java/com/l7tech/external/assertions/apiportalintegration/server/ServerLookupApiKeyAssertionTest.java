package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.LookupApiKeyAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.JAXBException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test the LookupApiKeyAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerLookupApiKeyAssertionTest {
    private static final String PLAN = "plan";
    private static final String SECRET = "secret";
    private static final String XML = "dummyxml";
    private static final String API_KEY_CV = "apikey";
    private static final String API_KEY_CV_VALUE = "abc123";
    private static final String SERVICE_ID = new Goid(0,1234).toHexString();
    private static final String STATUS = "active";
    private static final int VERSION = 7;
    private static final String LABEL = "someLabel";
    private static final String PLATFORM = "somePlatform";
    private static final String OAUTH_CALLBACK = "someOauthCallBackUrl";
    private static final String OAUTH_SCOPE = "someOauthScope";
    private static final String OAUTH_TYPE = "someOauthType";
    private static final String ACCOUNT_ID = "someOrgId";
    private static final String KEY_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.KEY_SUFFIX;
    private static final String FOUND_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.FOUND_SUFFIX;
    private static final String SERVICE_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.SERVICE_SUFFIX;
    private static final String PLAN_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.PLAN_SUFFIX;
    private static final String SECRET_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.SECRET_SUFFIX;
    private static final String STATUS_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.STATUS_SUFFIX;
    private static final String XML_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.XML_SUFFIX;
    private static final String LABEL_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.LABEL_SUFFIX;
    private static final String PLATFORM_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.PLATFORM_SUFFIX;
    private static final String OAUTH_CALLBACK_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.OAUTH_CALLBACK_SUFFIX;
    private static final String OAUTH_SCOPE_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.OAUTH_SCOPE_SUFFIX;
    private static final String OAUTH_TYPE_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.OAUTH_TYPE_SUFFIX;
    private static final String VERSION_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.VERSION_SUFFIX;
    private static final String ACCOUNT_ID_CV = LookupApiKeyAssertion.DEFAULT_PREFIX + "." + LookupApiKeyAssertion.ACCOUNT_PLAN_MAPPING_ID_SUFFIX;
    private LookupApiKeyAssertion assertion;
    private ServerLookupApiKeyAssertion serverAssertion;
    private PolicyEnforcementContext policyContext;
    private Map<String, String> serviceIdPlans;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private PortalGenericEntityManager<ApiKeyData> legacyApiKeyManager;
    @Mock
    private PortalGenericEntityManager<ApiKey> apiKeyManager;
    @Mock
    private ResourceTransformer<ApiKeyResource, ApiKey> transformer;
    @Mock
    private JAXBResourceMarshaller marshaller;

    @Before
    public void setup() throws Exception {
        assertion = new LookupApiKeyAssertion();
        assertion.setApiKey(API_KEY_CV_VALUE);
        assertion.setVariablePrefix(LookupApiKeyAssertion.DEFAULT_PREFIX);
        assertion.setServiceId(SERVICE_ID);
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        serviceIdPlans = new HashMap<String, String>();
        serviceIdPlans.put(SERVICE_ID, PLAN);
    }

    @Test(expected = PolicyAssertionException.class)
    public void constructorNullAssertionApiKey() throws Exception {
        assertion.setApiKey(null);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
    }

    @Test(expected = PolicyAssertionException.class)
    public void constructorEmptyAssertionApiKey() throws Exception {
        assertion.setApiKey("");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
    }

    @Test(expected = PolicyAssertionException.class)
    public void constructorNullAssertionPrefix() throws Exception {
        assertion.setVariablePrefix(null);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
    }

    @Test(expected = PolicyAssertionException.class)
    public void constructorEmptyAssertionPrefix() throws Exception {
        assertion.setVariablePrefix("");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
    }

    @Test
    public void checkRequestKeyFound() throws Exception {
        final ApiKey key = createKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID);
        final ApiKeyResource resource = createResource(key);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(key);
        when(transformer.entityToResource(key)).thenReturn(resource);
        when(marshaller.marshal(resource)).thenReturn(XML);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(SERVICE_ID, policyContext.getVariable(SERVICE_CV));
        assertEquals(PLAN, policyContext.getVariable(PLAN_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager, never()).find(anyString());
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    @Test
    public void checkRequestLegacyKeyFound() throws Exception {
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        when(legacyApiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(createLegacyKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(SERVICE_ID, policyContext.getVariable(SERVICE_CV));
        assertEquals(PLAN, policyContext.getVariable(PLAN_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager).find(API_KEY_CV_VALUE);
    }

    @Test
    public void checkRequestKeyFoundNoServiceIdOnAssertion() throws Exception {
        final ApiKey key = createKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID);
        final ApiKeyResource resource = createResource(key);
        assertion.setServiceId(null);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(key);
        when(transformer.entityToResource(key)).thenReturn(resource);
        when(marshaller.marshal(resource)).thenReturn(XML);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV);
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager, never()).find(anyString());
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    @Test
    public void checkRequestLegacyKeyFoundNoServiceIdOnAssertion() throws Exception {
        assertion.setServiceId(null);

        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        when(legacyApiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(createLegacyKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV);
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager).find(API_KEY_CV_VALUE);
    }

    @Test
    public void checkRequestKeyFoundBlankServiceIdOnAssertion() throws Exception {
        assertion.setServiceId("");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        final ApiKey key = createKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID);
        final ApiKeyResource resource = createResource(key);
        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(key);
        when(transformer.entityToResource(key)).thenReturn(resource);
        when(marshaller.marshal(resource)).thenReturn(XML);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV);
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager, never()).find(anyString());
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    @Test
    public void checkRequestLegacyKeyFoundBlankServiceIdOnAssertion() throws Exception {
        assertion.setServiceId("");

        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        when(legacyApiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(createLegacyKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV);
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager).find(API_KEY_CV_VALUE);
    }

    @Test
    public void checkRequestKeyFoundFromKeyContextVariable() throws Exception {
        final ApiKey key = createKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID);
        final ApiKeyResource resource = createResource(key);
        assertion.setApiKey("${" + API_KEY_CV + "}");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        policyContext.setVariable(API_KEY_CV, API_KEY_CV_VALUE);
        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(key);
        when(transformer.entityToResource(key)).thenReturn(resource);
        when(marshaller.marshal(resource)).thenReturn(XML);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(SERVICE_ID, policyContext.getVariable(SERVICE_CV));
        assertEquals(PLAN, policyContext.getVariable(PLAN_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager, never()).find(anyString());
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    @Test
    public void checkRequestLegacyKeyFoundFromKeyContextVariable() throws Exception {
        assertion.setApiKey("${" + API_KEY_CV + "}");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        policyContext.setVariable(API_KEY_CV, API_KEY_CV_VALUE);

        when(legacyApiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(createLegacyKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(SERVICE_ID, policyContext.getVariable(SERVICE_CV));
        assertEquals(PLAN, policyContext.getVariable(PLAN_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager).find(API_KEY_CV_VALUE);
    }

    @Test
    public void checkRequestKeyFoundFromServiceIdContextVariable() throws Exception {
        assertion.setServiceId(LookupApiKeyAssertion.DEFAULT_SERVICE_ID);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        final PublishedService service = new PublishedService();
        service.setGoid(Goid.parseGoid(SERVICE_ID));
        policyContext.setService(service);

        final ApiKey key = createKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID);
        final ApiKeyResource resource = createResource(key);
        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(key);
        when(transformer.entityToResource(key)).thenReturn(resource);
        when(marshaller.marshal(resource)).thenReturn(XML);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(SERVICE_ID, policyContext.getVariable(SERVICE_CV));
        assertEquals(PLAN, policyContext.getVariable(PLAN_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager, never()).find(anyString());
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    @Test
    public void checkRequestLegacyKeyFoundFromServiceIdContextVariable() throws Exception {
        assertion.setServiceId(LookupApiKeyAssertion.DEFAULT_SERVICE_ID);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        final PublishedService service = new PublishedService();
        service.setGoid(Goid.parseGoid(SERVICE_ID));
        policyContext.setService(service);

        when(legacyApiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(createLegacyKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertEquals(SERVICE_ID, policyContext.getVariable(SERVICE_CV));
        assertEquals(PLAN, policyContext.getVariable(PLAN_CV));
        assertEquals(XML, policyContext.getVariable(XML_CV));
        assertEquals(SECRET, policyContext.getVariable(SECRET_CV));
        assertEquals(STATUS, policyContext.getVariable(STATUS_CV));
        assertEquals(LABEL, policyContext.getVariable(LABEL_CV));
        assertEquals(PLATFORM, policyContext.getVariable(PLATFORM_CV));
        assertEquals(OAUTH_CALLBACK, policyContext.getVariable(OAUTH_CALLBACK_CV));
        assertEquals(OAUTH_SCOPE, policyContext.getVariable(OAUTH_SCOPE_CV));
        assertEquals(OAUTH_TYPE, policyContext.getVariable(OAUTH_TYPE_CV));
        assertEquals(String.valueOf(VERSION), policyContext.getVariable(VERSION_CV));
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager).find(API_KEY_CV_VALUE);
    }

    @Test
    public void checkRequestKeyNotFound() throws Exception {
        //key that we are looking for is different from key that is stored
        assertion.setApiKey("notfound");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertNotFound();
        assertEquals("notfound", policyContext.getVariable(KEY_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV, XML_CV, SECRET_CV, STATUS_CV);
        assertContextVariablesEmpty(LABEL_CV, PLATFORM_CV, OAUTH_CALLBACK_CV, OAUTH_SCOPE_CV, OAUTH_TYPE_CV);
        assertContextVariablesEmpty(VERSION_CV);
        verify(apiKeyManager).find("notfound");
        verify(legacyApiKeyManager).find("notfound");
    }

    @Test
    public void checkRequestKeyNotFoundAndNoServiceIdOnAssertion() throws Exception {
        //key that we are looking for is different from key that is stored
        assertion.setApiKey("notfound");
        assertion.setServiceId(null);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertNotFound();
        assertEquals("notfound", policyContext.getVariable(KEY_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV, XML_CV, SECRET_CV, STATUS_CV);
        verify(apiKeyManager).find("notfound");
        verify(legacyApiKeyManager).find("notfound");
    }

    @Test
    public void checkRequestKeyServiceIdDoesNotMatch() throws Exception {
        final ApiKey key = createKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID);
        final ApiKeyResource resource = createResource(key);
        assertion.setServiceId(SERVICE_ID + "x");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(key);
        when(transformer.entityToResource(key)).thenReturn(resource);
        when(marshaller.marshal(resource)).thenReturn(XML);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertNotFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV, XML_CV, SECRET_CV, STATUS_CV);
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager, never()).find(anyString());
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    @Test
    public void checkRequestLegacyKeyServiceIdDoesNotMatch() throws Exception {
        assertion.setServiceId(SERVICE_ID + "x");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        when(legacyApiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(createLegacyKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertNotFound();
        assertEquals(API_KEY_CV_VALUE, policyContext.getVariable(KEY_CV));
        assertContextVariablesEmpty(SERVICE_CV, PLAN_CV, XML_CV, SECRET_CV, STATUS_CV);
        verify(apiKeyManager).find(API_KEY_CV_VALUE);
        verify(legacyApiKeyManager).find(API_KEY_CV_VALUE);
    }

    @Test
    public void checkRequestNoKeyInContext() throws Exception {
        assertion.setApiKey("${" + API_KEY_CV + "}");
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        // context variable key not in context!
        policyContext.setVariable(API_KEY_CV, null);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertNotFound();
        assertContextVariablesEmpty(KEY_CV, SERVICE_CV, PLAN_CV, XML_CV, SECRET_CV, STATUS_CV);
        verify(apiKeyManager, never()).find(anyString());
        verify(legacyApiKeyManager, never()).find(Matchers.anyString());
    }

    /**
     * Service id is a context variable which does not resolve at runtime.
     *
     * @throws Exception
     */
    @Test
    public void checkRequestKeyFoundServiceIdContextVariableNotFound() throws Exception {
        assertion.setServiceId(LookupApiKeyAssertion.DEFAULT_SERVICE_ID);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        verify(apiKeyManager, never()).find(anyString());
        verify(legacyApiKeyManager, never()).find(Matchers.anyString());
    }

    @Test(expected = AssertionStatusException.class)
    public void checkRequestFindException() throws Exception {
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenThrow(new FindException());

        serverAssertion.checkRequest(policyContext);
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    @Test(expected = AssertionStatusException.class)
    public void checkRequestLegacyFindException() throws Exception {
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);

        when(legacyApiKeyManager.find(API_KEY_CV_VALUE)).thenThrow(new FindException());

        serverAssertion.checkRequest(policyContext);
    }

    @Test(expected = AssertionStatusException.class)
    public void checkRequestJAXBException() throws Exception {
        final ApiKey key = createKey(API_KEY_CV_VALUE, SECRET, STATUS, serviceIdPlans, LABEL, PLATFORM, OAUTH_CALLBACK, OAUTH_SCOPE, OAUTH_TYPE, VERSION, ACCOUNT_ID);
        final ApiKeyResource resource = createResource(key);
        serverAssertion = new ServerLookupApiKeyAssertion(assertion, legacyApiKeyManager, apiKeyManager, transformer, marshaller);
        when(apiKeyManager.find(API_KEY_CV_VALUE)).thenReturn(key);
        when(transformer.entityToResource(key)).thenReturn(resource);
        when(marshaller.marshal(resource)).thenThrow(new JAXBException("mocking exception"));

        serverAssertion.checkRequest(policyContext);
    }

    /**
     * @deprecated delete when no longer using ApiKeyData.
     */
    @Deprecated
    private ApiKeyData createLegacyKey(final String key, final String secret, final String status, final Map<String, String> serviceIdPlans,
                                       String label, String platform, String oauthCallBack, String oauthScope, String oauthType, int version, String accountId) {
        final ApiKeyData data = new ApiKeyData();
        data.setKey(key);
        data.setSecret(secret);
        data.setServiceIds(serviceIdPlans);
        data.setStatus(status);
        data.setXmlRepresentation(XML);
        data.setLabel(label);
        data.setPlatform(platform);
        data.setOauthCallbackUrl(oauthCallBack);
        data.setOauthScope(oauthScope);
        data.setOauthType(oauthType);
        data.setVersion(version);
        data.setAccountPlanMappingId(accountId);
        return data;
    }

    private ApiKey createKey(final String key, final String secret, final String status, final Map<String, String> serviceIdPlans,
                             String label, String platform, String oauthCallBack, String oauthScope, String oauthType, int version, String accountId) {
        final ApiKey apiKey = new ApiKey();
        apiKey.setName(key);
        apiKey.setSecret(secret);
        apiKey.setServiceIds(serviceIdPlans);
        apiKey.setStatus(status);
        apiKey.setLabel(label);
        apiKey.setPlatform(platform);
        apiKey.setOauthCallbackUrl(oauthCallBack);
        apiKey.setOauthScope(oauthScope);
        apiKey.setOauthType(oauthType);
        apiKey.setVersion(version);
        apiKey.setAccountPlanMappingId(accountId);
        return apiKey;
    }

    private ApiKeyResource createResource(final ApiKey key) {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey(key.getName());
        resource.setLabel(key.getLabel());
        resource.setPlatform(key.getPlatform());
        resource.setSecret(key.getSecret());
        resource.setStatus(key.getStatus());
        resource.setApis(key.getServiceIds());
        resource.setAccountPlanMappingId(key.getAccountPlanMappingId());
        resource.setSecurity(new SecurityDetails(new OAuthDetails(key.getOauthCallbackUrl(), key.getOauthScope(), key.getOauthType())));
        return resource;
    }

    private void assertContextVariablesEmpty(final String... contextVariables) throws Exception {
        for (final String contextVariable : contextVariables) {
            assertTrue(((String) policyContext.getVariable(contextVariable)).isEmpty());
        }
    }

    private void assertFound() throws Exception {
        assertTrue((Boolean) policyContext.getVariable(FOUND_CV));
    }

    private void assertNotFound() throws Exception {
        assertFalse((Boolean) policyContext.getVariable(FOUND_CV));
    }
}
