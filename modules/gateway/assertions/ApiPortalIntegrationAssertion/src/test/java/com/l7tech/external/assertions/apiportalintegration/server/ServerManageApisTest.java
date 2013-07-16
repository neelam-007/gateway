package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
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
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import javax.xml.bind.JAXBException;
import java.util.*;

import static com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion.*;
import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.ROOT_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerManageApisTest {
    private ServerManagePortalResourceAssertion serverAssertion;
    private ManagePortalResourceAssertion assertion;
    private PolicyEnforcementContext policyContext;
    private List<ApiResource> apis;
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
                keyLegacyResourceHandler, accountPlanResourceHandler, policyHelper, policyValidationMarshaller);
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        apis = new ArrayList<ApiResource>();
        expectedFilters = new HashMap<String, String>();
    }

    @Test
    public void checkRequestGetAllAPIs() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");
        final ApiResource api1 = new ApiResource("a1", "g1", "1111");
        final ApiResource api2 = new ApiResource("a2", "g2", "2222");
        apis.add(api1);
        apis.add(api2);
        when(apiResourceHandler.get(anyMap())).thenReturn(apis);
        when(resourceMarshaller.marshal(any(ApiListResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiListResourceMatcher(api1, api2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAllAPIsFilterByApiGroup() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");
        policyContext.setVariable(OPTION_API_GROUP, "testGroup");
        final ApiResource matchGroup = new ApiResource("a1", "testGroup", "1111");
        apis.add(matchGroup);
        when(apiResourceHandler.get(anyMap())).thenReturn(apis);
        when(resourceMarshaller.marshal(any(ApiListResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("apiGroup", "testGroup");
        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiListResourceMatcher(matchGroup)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAllAPIsNone() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");
        when(apiResourceHandler.get(anyMap())).thenReturn(Collections.<ApiResource>emptyList());
        when(resourceMarshaller.marshal(any(ApiListResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAllAPIsFilterByApiGroupNone() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");
        policyContext.setVariable(OPTION_API_GROUP, "testGroup");
        when(apiResourceHandler.get(anyMap())).thenReturn(apis);
        when(resourceMarshaller.marshal(any(ApiListResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("apiGroup", "testGroup");
        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAPI() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis/a1");
        final ApiResource api = new ApiResource("a1", "g1", "1111");
        apis.add(api);
        when(apiResourceHandler.get(anyMap())).thenReturn(apis);
        when(resourceMarshaller.marshal(any(ApiResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "a1");
        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiListResourceMatcher(api)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAPINotFound() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis/a1");
        when(apiResourceHandler.get(anyMap())).thenReturn(Collections.<ApiResource>emptyList());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "a1");
        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find Api with apiId=a1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAPITooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis/a1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - /1/apis/a1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAPIsFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");
        when(apiResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(any(ApiListResource.class));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAPIFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis/a1");
        when(apiResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "a1");
        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAPIsJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");
        final ApiResource api = new ApiResource("a1", "g1", "1111");
        apis.add(api);
        when(apiResourceHandler.get(anyMap())).thenReturn(apis);
        when(resourceMarshaller.marshal(any(ApiListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiListResourceMatcher(api)));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAPIJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis/a1");
        final ApiResource api = new ApiResource("a1", "g1", "1111");
        apis.add(api);
        when(apiResourceHandler.get(anyMap())).thenReturn(apis);
        when(resourceMarshaller.marshal(any(ApiResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "a1");
        verify(apiResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiListResourceMatcher(api)));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
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

    private class ApiListResourceMatcher extends ArgumentMatcher<ApiListResource> {
        final List<ApiResource> expectedResources;

        public ApiListResourceMatcher(final ApiResource... resources) {
            this.expectedResources = Arrays.asList(resources);
        }

        @Override
        public boolean matches(final Object o) {
            final ApiListResource list = (ApiListResource) o;
            if (expectedResources.size() == list.getApis().size() && expectedResources.containsAll(list.getApis())) {
                return true;
            }
            return false;
        }
    }
}
