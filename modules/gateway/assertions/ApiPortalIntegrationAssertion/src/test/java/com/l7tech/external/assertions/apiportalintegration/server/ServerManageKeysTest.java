package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion.*;
import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.ROOT_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerManageKeysTest {
    private static final String INPUT = "inputXml";
    private static final String OUTPUT = "outputXml";
    private ServerManagePortalResourceAssertion serverAssertion;
    private ManagePortalResourceAssertion assertion;
    private PolicyEnforcementContext policyContext;
    private Map<String, String> expectedFilters;
    @Mock
    private JAXBResourceMarshaller resourceMarshaller;
    @Mock
    private JAXBResourceUnmarshaller resourceUnmarshaller;
    @Mock
    private ApiResourceHandler apiResourceHandler;
    @Mock
    private ApiPlanResourceHandler planResourceHandler;
    @Mock
    private ApiKeyResourceHandler keyResourceHandler;
    @Mock
    private ApiKeyDataResourceHandler keyLegacyResourceHandler;
    @Mock
    private AccountPlanResourceHandler accountPlanResourceHandler;
    @Mock
    private ApiFragmentResourceHandler apiFragmentResourceHandler;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private PolicyVersionManager policyVersionManager;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private AssertionLicense licenseManager;
    @Mock
    private PolicyValidator policyValidator;
    @Mock
    private PolicyValidationMarshaller policyValidationMarshaller;
    private PolicyHelper policyHelper;

    @Before
    public void setup() throws Exception {
        assertion = new ManagePortalResourceAssertion();
        policyHelper = new PolicyHelper(policyManager,policyVersionManager,transactionManager,licenseManager,policyValidator);
        serverAssertion = new ServerManagePortalResourceAssertion(assertion,
                resourceMarshaller, resourceUnmarshaller, apiResourceHandler, planResourceHandler, keyResourceHandler,
                keyLegacyResourceHandler, accountPlanResourceHandler, apiFragmentResourceHandler, policyHelper,
                policyValidationMarshaller);
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        expectedFilters = new HashMap<String, String>();
    }

    @Test
     public void getKey() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        final List<ApiKeyResource> resources = createDefaultResources();
        when(keyResourceHandler.get(anyMap())).thenReturn(resources);
        when(resourceMarshaller.marshal(any(ApiKeyListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
        verify(keyResourceHandler).get(anyMap());
        verify(resourceMarshaller).marshal(any(ApiKeyListResource.class));
    }

    @Test
    public void getKeys() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        final List<ApiKeyResource> resources = createDefaultResources();
        when(keyResourceHandler.get(anyMap())).thenReturn(resources);
        when(resourceMarshaller.marshal(any(ApiKeyListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
        verify(keyResourceHandler).get(anyMap());
        verify(resourceMarshaller).marshal(any(ApiKeyListResource.class));
    }

    @Test
    public void getKeyNotFound() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find ApiKey with key=k1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).get(anyMap());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void getKeyFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        when(keyResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).get(anyMap());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void getKeyJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        final ApiKeyResource resource = createDefaultResource();
        final List<ApiKeyResource> resources = createDefaultResources();
        when(keyResourceHandler.get(anyMap())).thenReturn(resources);
        when(resourceMarshaller.marshal(any(ApiKeyListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).get(anyMap());
        verify(resourceMarshaller).marshal(any(ApiKeyListResource.class));
    }

    @Test
    public void getKeyTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - /1/api/keys/k1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler, never()).get(anyString());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKey() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final List<ApiKeyResource> resources = createDefaultResources();
        final ApiKeyListResource apiKeyListResource = createDefaultKeyResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenReturn(apiKeyListResource);
        when(keyResourceHandler.put(anyList(), anyBoolean())).thenReturn(resources);
        when(resourceMarshaller.marshal(any(ApiKeyListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler).put(anyList(), anyBoolean());
        verify(resourceMarshaller).marshal(any(ApiKeyListResource.class));
    }

    @Test
    public void putKeyUnmarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler, never()).put(Matchers.<ApiKeyResource>any());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyMarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final List<ApiKeyResource> resources = createDefaultResources();
        final ApiKeyListResource apiKeyListResource = createDefaultKeyResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenReturn(apiKeyListResource);
        when(keyResourceHandler.put(anyList(), anyBoolean())).thenReturn(resources);
        when(resourceMarshaller.marshal(any(ApiKeyListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        //assertEquals(AssertionStatus.FAILED, assertionStatus); - 2.1 implementation
        //assertContextVariableDoesNotExist(RESPONSE_STATUS); - 2.1 implementation
        //assertContextVariableDoesNotExist(RESPONSE_DETAIL); - 2.1 implementation
        //assertContextVariableDoesNotExist(RESPONSE_RESOURCE); - 2.1 implementation
        //for 2.2 we make it the same behaviour as API Plans, record has already been persisted so we even
        //if there is a JAXB exception we will still proceed with an empty list
        assertEquals(AssertionStatus.NONE, assertionStatus);

        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("N/A", (String) policyContext.getVariable(RESPONSE_RESOURCE));

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler).put(anyList(), anyBoolean());
        verify(resourceMarshaller).marshal(any(ApiKeyListResource.class));
    }

    @Test
    public void putKeyFindException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);        
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenReturn(createDefaultKeyResource());
        when(keyResourceHandler.put(anyList(), anyBoolean())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeySaveException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenReturn(createDefaultKeyResource());
        when(keyResourceHandler.put(anyList(), anyBoolean())).thenThrow(new SaveException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyUpdateException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiKeyListResource apiKeyListResource = createDefaultKeyResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenReturn(apiKeyListResource);
        when(keyResourceHandler.put(anyList(), anyBoolean())).thenThrow(new UpdateException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyMissingKey() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final List<ApiKeyResource> resources = createDefaultResources();     
        resources.get(0).setKey(null);
        final ApiKeyListResource apiKeyListResource = new ApiKeyListResource(resources);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenReturn(apiKeyListResource);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("API Key missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler, never()).put(Matchers.<ApiKeyResource>any());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyMissingSecret() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final List<ApiKeyResource> resources = createDefaultResources();
        resources.get(0).setSecret(null);
        final ApiKeyListResource apiKeyListResource = new ApiKeyListResource(resources);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyListResource.class)).thenReturn(apiKeyListResource);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("API Secret missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyListResource.class);
        verify(keyResourceHandler, never()).put(Matchers.<ApiKeyResource>any());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/extra");
        policyContext.setVariable(RESOURCE, INPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: PUT - /1/api/keys/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller, never()).unmarshal(anyString(), Matchers.<Class>any());
        verify(keyResourceHandler, never()).put(Matchers.<ApiKeyResource>any());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void deleteKey() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).delete("k1");
    }

    @Test
    public void deleteKeyFindException() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        doThrow(new FindException("mocking exception")).when(keyResourceHandler).delete("k1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find ApiKey with key=k1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).delete("k1");
    }

    @Test
    public void deleteKeyDeleteException() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        doThrow(new DeleteException("mocking exception")).when(keyResourceHandler).delete("k1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).delete("k1");
    }

    @Test
    public void deleteKeyMissingId() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: DELETE - /1/api/keys", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler, never()).delete(anyString());
    }

    @Test
    public void deleteKeyTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: DELETE - /1/api/keys/k1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler, never()).delete(anyString());
    }

    @Test
    public void invalidOperation() throws Exception{
        policyContext.setVariable(OPERATION, "POST");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: POST - /1/api/keys", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler, never()).delete(anyString());
    }

    private ApiKeyResource createDefaultResource() {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey("k1");
        resource.setLabel("label");
        resource.setPlatform("platform");
        resource.setSecret("secret");
        resource.setStatus("active");
        resource.setAccountPlanMappingId("l7org");
        resource.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        resource.setApis(services);
        return resource;
    }

    private List<ApiKeyResource> createDefaultResources() {
        List<ApiKeyResource> resources = new ArrayList<ApiKeyResource>();
        resources.add(createDefaultResource());
        return resources;
    }

    private ApiKeyListResource createDefaultKeyResource() {
        return new ApiKeyListResource( createDefaultResources());
    }

    private void assertContextVariableDoesNotExist(final String name) {
        try {
            policyContext.getVariable(name);
            fail("Found unexpected context variable with name=" + name);
        } catch (final NoSuchVariableException e) {
            // expected
        }
    }
}
