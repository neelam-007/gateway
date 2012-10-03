package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerManagePortalResourceAssertionTest {
    private ServerManagePortalResourceAssertion serverAssertion;
    private ManagePortalResourceAssertion assertion;
    private PolicyEnforcementContext policyContext;
    @Mock
    private PortalManagedServiceManager portalManagedServiceManager;
    @Mock
    private JAXBResourceMarshaller resourceMarshaller;
    @Mock
    private JAXBResourceUnmarshaller resourceUnmarshaller;
    @Mock
    private PortalGenericEntityManager<ApiPlan> apiPlanManager;
    @Mock
    private ResourceTransformer<ApiPlanResource, ApiPlan> planTransformer;
    @Mock
    private ResourceTransformer<ApiResource, PortalManagedService> apiTransformer;
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
    }

    @Test
    public void checkRequestNoOperation() throws Exception {
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(portalManagedServiceManager, never()).findAll();
        verify(resourceMarshaller, never()).marshal(any(ApiListResource.class));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Context variable pman.operation not set", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestInvalidOperation() throws Exception {
        policyContext.setVariable(OPERATION, "INVALID");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(portalManagedServiceManager, never()).findAll();
        verify(resourceMarshaller, never()).marshal(any(ApiListResource.class));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Context variable pman.operation is not a valid http method: INVALID", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestOperationNotSupported() throws Exception {
        policyContext.setVariable(OPERATION, "POST");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "apis");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(portalManagedServiceManager, never()).findAll();
        verify(resourceMarshaller, never()).marshal(any(ApiListResource.class));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: POST - /1/apis", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestNoResourceUri() throws Exception {
        policyContext.setVariable(OPERATION, "GET");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(portalManagedServiceManager, never()).findAll();
        verify(resourceMarshaller, never()).marshal(any(ApiListResource.class));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Context variable pman.resUri not set", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestResourceUriDoesNotContainRootUri() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, "invalid");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid resource uri: invalid", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestResourceUriDoesNotContainResourceType() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - " + ROOT_URI, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestUnrecognizedResourceType() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "idunno");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(portalManagedServiceManager, never()).findAll();
        verify(resourceMarshaller, never()).marshal(any(ApiListResource.class));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - " + ROOT_URI + "idunno", (String) policyContext.getVariable(RESPONSE_DETAIL));
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
}
