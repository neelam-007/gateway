package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.bind.JAXBException;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion.*;
import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.ROOT_URI;
import static org.junit.Assert.*;
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

    @Before
    public void setup() throws Exception {
        assertion = new ManagePortalResourceAssertion();
        serverAssertion = new ServerManagePortalResourceAssertion(assertion,
                resourceMarshaller, resourceUnmarshaller, apiResourceHandler, planResourceHandler, keyResourceHandler);
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        expectedFilters = new HashMap<String, String>();
    }

    @Test
    public void getKey() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        final ApiKeyResource resource = createDefaultResource();
        when(keyResourceHandler.get("k1")).thenReturn(resource);
        when(resourceMarshaller.marshal(resource)).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
        verify(keyResourceHandler).get("k1");
        verify(resourceMarshaller).marshal(resource);
    }

    @Test
    public void getKeyNotFound() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        when(keyResourceHandler.get("k1")).thenReturn(null);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find ApiKey with key=k1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).get("k1");
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void getKeyFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        when(keyResourceHandler.get("k1")).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).get("k1");
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void getKeyJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys/k1");
        final ApiKeyResource resource = createDefaultResource();
        when(keyResourceHandler.get("k1")).thenReturn(resource);
        when(resourceMarshaller.marshal(resource)).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler).get("k1");
        verify(resourceMarshaller).marshal(resource);
    }

    @Test
    public void getKeyMissingId() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - /1/api/keys", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(keyResourceHandler, never()).get(anyString());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
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
        final ApiKeyResource resource = createDefaultResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyResource.class)).thenReturn(resource);
        when(keyResourceHandler.put(resource)).thenReturn(resource);
        when(resourceMarshaller.marshal(resource)).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyResource.class);
        verify(keyResourceHandler).put(resource);
        verify(resourceMarshaller).marshal(resource);
    }

    @Test
    public void putKeyUnmarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyResource.class)).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyResource.class);
        verify(keyResourceHandler, never()).put(Matchers.<ApiKeyResource>any());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyMarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiKeyResource resource = createDefaultResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyResource.class)).thenReturn(resource);
        when(keyResourceHandler.put(resource)).thenReturn(resource);
        when(resourceMarshaller.marshal(resource)).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyResource.class);
        verify(keyResourceHandler).put(resource);
        verify(resourceMarshaller).marshal(resource);
    }

    @Test
    public void putKeyFindException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiKeyResource resource = createDefaultResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyResource.class)).thenReturn(resource);
        when(keyResourceHandler.put(resource)).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyResource.class);
        verify(keyResourceHandler).put(resource);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeySaveException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiKeyResource resource = createDefaultResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyResource.class)).thenReturn(resource);
        when(keyResourceHandler.put(resource)).thenThrow(new SaveException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyResource.class);
        verify(keyResourceHandler).put(resource);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyUpdateException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiKeyResource resource = createDefaultResource();
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyResource.class)).thenReturn(resource);
        when(keyResourceHandler.put(resource)).thenThrow(new UpdateException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyResource.class);
        verify(keyResourceHandler).put(resource);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
    }

    @Test
    public void putKeyMissingKey() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/keys");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiKeyResource resource = createDefaultResource();
        resource.setKey(null);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiKeyResource.class)).thenReturn(resource);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Resource id missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(resourceUnmarshaller).unmarshal(INPUT, ApiKeyResource.class);
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
        resource.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        resource.setApis(services);
        return resource;
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
