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
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import javax.xml.bind.JAXBException;
import java.util.*;

import static com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion.*;
import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.ROOT_URI;
import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerManagePlansTest {
    private static final Date LAST_UPDATE = new Date();
    private static final String INPUT = "<input>dummy</input>";
    private static final String OUTPUT = "<output>dummy</output>";
    private ServerManagePortalResourceAssertion serverAssertion;
    private ManagePortalResourceAssertion assertion;
    private PolicyEnforcementContext policyContext;
    private List<ApiPlanResource> apiPlanResources;
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
        apiPlanResources = new ArrayList<ApiPlanResource>();
        expectedFilters = new HashMap<String, String>();
    }

    @Test
    public void checkRequestGetAPIPlan() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1");
        final ApiPlanResource resource = new ApiPlanResource("p1", "pName", LAST_UPDATE, "policy xml", true);
        apiPlanResources.add(resource);
        when(planResourceHandler.get(anyMap())).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanResource.class))).thenReturn("plan xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("plan xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAPIPlanNotFound() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1");
        when(planResourceHandler.get(anyMap())).thenReturn(Collections.<ApiPlanResource>emptyList());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find ApiPlan with planId=p1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAPIPlans() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policyXml1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policyXml2", true);
        apiPlanResources.add(resource1);
        apiPlanResources.add(resource2);
        when(planResourceHandler.get(anyMap())).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenReturn("plans xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource1, resource2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("plans xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAllAPIPlansNone() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        apiPlanResources.clear();
        when(planResourceHandler.get(anyMap())).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenReturn("plans xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("plans xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetApiPlanFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1");
        when(planResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetApiPlanJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1");
        final ApiPlanResource resource = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policy xml", true);
        apiPlanResources.add(resource);
        when(planResourceHandler.get(anyMap())).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetApiPlanTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler, never()).get(anyMap());
        verify(resourceMarshaller, never()).marshal(any(ApiPlanResource.class));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - " + ROOT_URI + "api/plans/p1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAPIPlansFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        when(planResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(any(ApiPlanListResource.class));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAPIPlansJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        final ApiPlanResource resource = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policyXml1", true);
        apiPlanResources.add(resource);
        when(planResourceHandler.get(anyMap())).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansSaveException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        apiPlanResources.add(new ApiPlanResource("p1", "pName", null, "policy xml", true));
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(new ApiPlanListResource(apiPlanResources));
        when(planResourceHandler.put(anyList(), anyBoolean())).thenThrow(new SaveException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler).put(apiPlanResources, false);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansUnmarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler,never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansMarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiPlanResource resource = new ApiPlanResource("p1", "pName", null, "policy xml", true);
        apiPlanResources.add(resource);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(new ApiPlanListResource(apiPlanResources));
        when(planResourceHandler.put(apiPlanResources, false)).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler).put(apiPlanResources, false);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("N/A", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansEmptyPlanId() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        apiPlanResources.add(new ApiPlanResource("", "pName", null, "policy xml", false));
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(new ApiPlanListResource(apiPlanResources));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler, never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Resource id missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansNullPlanId() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        apiPlanResources.add(new ApiPlanResource(null, "pName", null, "policy xml", false));
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(new ApiPlanListResource(apiPlanResources));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler, never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Resource id missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/extra");
        policyContext.setVariable(RESOURCE, INPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller, never()).unmarshal(anyString(), Matchers.<Class>any());
        verify(planResourceHandler, never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: PUT - /1/api/plans/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateApiPlans() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", null, "policy xml 1", true);
        apiPlanResources.add(resource1);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", null, "policy xml 2", true);
        apiPlanResources.add(resource2);
        final ApiPlanListResource listResource = new ApiPlanListResource(apiPlanResources);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(listResource);
        when(planResourceHandler.put(apiPlanResources, false)).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler).put(apiPlanResources, false);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource1, resource2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansNone() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"></l7:ApiPlans>";
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, xml);
        apiPlanResources.clear();
        final ApiPlanListResource listResource = new ApiPlanListResource(apiPlanResources);
        when(resourceUnmarshaller.unmarshal(xml, ApiPlanListResource.class)).thenReturn(listResource);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenReturn(xml);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(xml, ApiPlanListResource.class);
        verify(planResourceHandler,never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(xml, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @BugId("APIM-522")
    @Test
    public void checkRequestAddOrUpdateApiPlansNoneRemoveOmitted() throws Exception {
        final String xml = "<l7:ApiPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"></l7:ApiPlans>";
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, xml);
        policyContext.setVariable(OPTION_REMOVE_OMITTED, "true");
        apiPlanResources.clear();
        final ApiPlanListResource listResource = new ApiPlanListResource(apiPlanResources);
        when(resourceUnmarshaller.unmarshal(xml, ApiPlanListResource.class)).thenReturn(listResource);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenReturn(xml);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(xml, ApiPlanListResource.class);
        verify(planResourceHandler).put(apiPlanResources, true);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(xml, policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestAddOrUpdatApiPlansUpdateException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        apiPlanResources.add(new ApiPlanResource("p1", "pName1", null, "policy xml 1", true));
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(new ApiPlanListResource(apiPlanResources));
        when(planResourceHandler.put(apiPlanResources, false)).thenThrow(new UpdateException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler).put(apiPlanResources, false);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateApiPlansDeleteOmitted() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        policyContext.setVariable(OPTION_REMOVE_OMITTED, true);
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", null, "policy xml 1", true);
        apiPlanResources.add(resource1);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", null, "policy xml 2", false);
        apiPlanResources.add(resource2);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(new ApiPlanListResource(apiPlanResources));
        when(planResourceHandler.put(apiPlanResources, true)).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler).put(apiPlanResources, true);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource1, resource2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    /**
     * If option is not a valid boolean, should not delete any plans.
     */
    @Test
    public void checkRequestAddOrUpdateApiPlansDeleteOmittedNotValidBoolean() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        policyContext.setVariable(OPTION_REMOVE_OMITTED, "invalid");
        final ApiPlanResource resource = new ApiPlanResource("p1", "pName1", null, "policy xml 1", true);
        apiPlanResources.add(resource);
        when(resourceUnmarshaller.unmarshal(INPUT, ApiPlanListResource.class)).thenReturn(new ApiPlanListResource(apiPlanResources));
        when(planResourceHandler.put(apiPlanResources, false)).thenReturn(apiPlanResources);
        when(resourceMarshaller.marshal(any(ApiPlanListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, ApiPlanListResource.class);
        verify(planResourceHandler).put(apiPlanResources, false);
        verify(resourceMarshaller).marshal(argThat(new ApiPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestDeleteApiPlan() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler).delete("p1");
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteApiPlanDeleteException() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1");
        doThrow(new DeleteException("mocking exception")).when(planResourceHandler).delete("p1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler).delete("p1");
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteApiPlanNotFound() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1");
        doThrow(new FindException("mocking exception")).when(planResourceHandler).delete("p1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler).delete("p1");
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find ApiPlan with planId=p1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteApiPlanTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans/p1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler, never()).delete(anyString());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: DELETE - /1/api/plans/p1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteApiPlanMissingPlanId() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "api/plans");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(planResourceHandler, never()).delete(anyString());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: DELETE - /1/api/plans", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    private void assertContextVariableDoesNotExist(final String name) {
        try {
            policyContext.getVariable(name);
            fail("Found unexpected context variable with name=" + name);
        } catch (final NoSuchVariableException e) {
            // expected
        }
    }

    private class ApiPlanListResourceMatcher extends ArgumentMatcher<ApiPlanListResource> {
        final List<ApiPlanResource> expectedPlans;

        public ApiPlanListResourceMatcher(final ApiPlanResource... resources) {
            this.expectedPlans = Arrays.asList(resources);
        }

        @Override
        public boolean matches(final Object o) {
            final ApiPlanListResource list = (ApiPlanListResource) o;
            if (expectedPlans.size() == list.getApiPlans().size() && expectedPlans.containsAll(list.getApiPlans())) {
                return true;
            }
            return false;
        }
    }
}
