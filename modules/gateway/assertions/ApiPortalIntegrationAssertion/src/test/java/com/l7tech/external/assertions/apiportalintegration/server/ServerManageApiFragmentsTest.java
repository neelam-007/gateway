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
import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.API_FRAGMENTS_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerManageApiFragmentsTest {
    private ServerManagePortalResourceAssertion serverAssertion;
    private ManagePortalResourceAssertion assertion;
    private PolicyEnforcementContext policyContext;
    private List<ApiFragmentResource> apiFragments;
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
        apiFragments = new ArrayList<ApiFragmentResource>();
        expectedFilters = new HashMap<String, String>();
    }

    @Test
    public void checkRequestGetAllApiFragments() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI);
        final ApiFragmentResource apiFragment1 = new ApiFragmentResource("a1","id1", "true", "details");
        final ApiFragmentResource apiFragment2 = new ApiFragmentResource("a2","id2", "true", "details");
        apiFragments.add(apiFragment1);
        apiFragments.add(apiFragment2);
        when(apiFragmentResourceHandler.get(anyMap())).thenReturn(apiFragments);
        when(resourceMarshaller.marshal(any(ApiFragmentListResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiFragmentListResourceMatcher(apiFragment1, apiFragment2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAllApiFragmentsNone() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI);
        when(apiFragmentResourceHandler.get(anyMap())).thenReturn(Collections.<ApiFragmentResource>emptyList());
        when(resourceMarshaller.marshal(any(ApiListResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiFragmentListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetApiFragment() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI + "/a1");
        final ApiFragmentResource apiFragment = new ApiFragmentResource("a1","id1", "true", "details");
        apiFragments.add(apiFragment);
        when(apiFragmentResourceHandler.get(anyMap())).thenReturn(apiFragments);
        when(resourceMarshaller.marshal(any(ApiResource.class))).thenReturn("the xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("guid", "a1");
        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiFragmentListResourceMatcher(apiFragment)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("the xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetApiFragmentNotFound() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI + "/a1");
        when(apiFragmentResourceHandler.get(anyMap())).thenReturn(Collections.<ApiFragmentResource>emptyList());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("guid", "a1");
        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find Api Fragment with guid=a1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetApiFragmentsTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI +API_FRAGMENTS_URI + "/a1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - /1/api/fragments/a1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllApiFragmentsFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI);
        when(apiFragmentResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(any(ApiListResource.class));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetApiFragmentFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI + "/a1");
        when(apiFragmentResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("guid", "a1");
        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllApiFragmentsJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI);
        final ApiFragmentResource apiFragment = new ApiFragmentResource("a1","id1", "true", "details");
        apiFragments.add(apiFragment);
        when(apiFragmentResourceHandler.get(anyMap())).thenReturn(apiFragments);
        when(resourceMarshaller.marshal(any(ApiListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiFragmentListResourceMatcher(apiFragment)));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetApiFragmentJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + API_FRAGMENTS_URI + "/a1");
        final ApiFragmentResource apiFragment = new ApiFragmentResource("a1","id1", "true", "details");
        apiFragments.add(apiFragment);
        when(apiFragmentResourceHandler.get(anyMap())).thenReturn(apiFragments);
        when(resourceMarshaller.marshal(any(ApiResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("guid", "a1");
        verify(apiFragmentResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new ApiFragmentListResourceMatcher(apiFragment)));
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

    private class ApiFragmentListResourceMatcher extends ArgumentMatcher<ApiFragmentListResource> {
        final List<ApiFragmentResource> expectedResources;

        public ApiFragmentListResourceMatcher(final ApiFragmentResource... resources) {
            this.expectedResources = Arrays.asList(resources);
        }

        @Override
        public boolean matches(final Object o) {
            final ApiFragmentListResource list = (ApiFragmentListResource) o;
            if (expectedResources.size() == list.getApiFragments().size() &&
                    expectedResources.containsAll(list.getApiFragments())) {
                return true;
            }
            return false;
        }
    }
}
