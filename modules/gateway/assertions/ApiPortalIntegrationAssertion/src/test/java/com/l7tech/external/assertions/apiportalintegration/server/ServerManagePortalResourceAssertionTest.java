package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

import static com.l7tech.external.assertions.apiportalintegration.ManagePortalResourceAssertion.*;
import static com.l7tech.external.assertions.apiportalintegration.server.ServerManagePortalResourceAssertion.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

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
        policyHelper = new PolicyHelper(policyManager, policyVersionManager, transactionManager, licenseManager, policyValidator);
        serverAssertion = new ServerManagePortalResourceAssertion(assertion,
                resourceMarshaller, resourceUnmarshaller, apiResourceHandler, planResourceHandler, keyResourceHandler,
                keyLegacyResourceHandler, accountPlanResourceHandler, apiFragmentResourceHandler, policyHelper,
                policyValidationMarshaller);
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

    @Test
    public void checkGateway() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "gateway");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(apiResourceHandler).get(anyMap());
        verify(planResourceHandler).get(anyMap());
        verify(keyResourceHandler).get(anyMap());
        verify(keyLegacyResourceHandler).get(anyMap());
        verify(accountPlanResourceHandler).get(anyMap());
        verify(apiFragmentResourceHandler).get(anyMap());
        verify(apiResourceHandler).getCacheItems();
        verify(planResourceHandler).getCacheItems();
        verify(keyResourceHandler).getCacheItems();
        verify(keyLegacyResourceHandler).getCacheItems();
        verify(accountPlanResourceHandler).getCacheItems();
        verify(apiFragmentResourceHandler).getCacheItems();
        verify(resourceMarshaller).marshal(any(GatewayResource.class));
    }

    @Test
    public void checkGatewayInvalid() throws Exception {
        policyContext.setVariable(OPERATION, "INVALID");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "gateway");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Context variable pman.operation is not a valid http method: INVALID", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
        verify(apiResourceHandler, never()).get(anyMap());
        verify(planResourceHandler, never()).get(anyMap());
        verify(keyResourceHandler, never()).get(anyMap());
        verify(keyLegacyResourceHandler, never()).get(anyMap());
        verify(accountPlanResourceHandler, never()).get(anyMap());
        verify(apiFragmentResourceHandler, never()).get(anyMap());
        verify(apiResourceHandler, never()).getCacheItems();
        verify(planResourceHandler, never()).getCacheItems();
        verify(keyResourceHandler, never()).getCacheItems();
        verify(keyLegacyResourceHandler, never()).getCacheItems();
        verify(accountPlanResourceHandler, never()).getCacheItems();
        verify(apiFragmentResourceHandler, never()).getCacheItems();
        verify(resourceMarshaller, never()).marshal(any(GatewayResource.class));
    }

    @Test
    public void checkPolicyUpdate() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_UPDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");
        policyContext.setVariable(RESOURCE, POLICY_XML);
        when(policyManager.findByGuid("xxx")).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, "xxx", POLICY_XML, false));
        PolicyValidatorResult policyValidatorResult = new PolicyValidatorResult();
        List<Integer> list = new ArrayList<Integer>();
        //list.add(new Integer(1));
        policyValidatorResult.addWarning(new PolicyValidatorResult.Warning(list, 1, "", new Throwable()));
        when(policyValidator.validate(any(Assertion.class), any(PolicyValidationContext.class), any(AssertionLicense.class))).thenReturn(policyValidatorResult);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(policyManager, times(2)).findByGuid(anyString());
    }

    @Test
    public void checkPolicyValidate() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_VALIDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");
        policyContext.setVariable(RESOURCE, POLICY_XML);
        when(policyManager.findByGuid("xxx")).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, "xxx", POLICY_XML, false));
        PolicyValidatorResult policyValidatorResult = new PolicyValidatorResult();
        List<Integer> list = new ArrayList<Integer>();
        //list.add(new Integer(1));
        policyValidatorResult.addWarning(new PolicyValidatorResult.Warning(list, 1, "", new Throwable()));
        when(policyValidator.validate(any(Assertion.class), any(PolicyValidationContext.class), any(AssertionLicense.class))).thenReturn(policyValidatorResult);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(policyManager).findByGuid(anyString());
    }

    @Test
    public void checkPolicyValidateFailValidation() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_VALIDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");
        policyContext.setVariable(RESOURCE, POLICY_XML);
        when(policyManager.findByGuid("xxx")).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, "xxx", POLICY_XML, false));
        PolicyValidatorResult policyValidatorResult = new PolicyValidatorResult();
        List<Integer> list = new ArrayList<Integer>();
        list.add(new Integer(1));
        policyValidatorResult.addError(new PolicyValidatorResult.Error(list, 1, "", new Throwable()));
        when(policyValidator.validate(any(Assertion.class), any(PolicyValidationContext.class), any(AssertionLicense.class))).thenReturn(policyValidatorResult);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertEquals(new Integer(500), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(policyManager).findByGuid(anyString());
    }

    @Test
    public void checkPolicyValidateFailValidation2() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_VALIDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");
        policyContext.setVariable(RESOURCE, POLICY_XML);
        when(policyManager.findByGuid("xxx")).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, "xxx", POLICY_XML, false));
        PolicyValidatorResult policyValidatorResult = new PolicyValidatorResult();
        List<Integer> list = new ArrayList<Integer>();
        list.add(new Integer(1));
        policyValidatorResult.addError(new PolicyValidatorResult.Error(list, 1, "", new Throwable()));
        when(policyValidator.validate(any(Assertion.class), any(PolicyValidationContext.class), any(AssertionLicense.class))).thenThrow(new InterruptedException(""));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertEquals(new Integer(500), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(policyManager).findByGuid(anyString());
    }

    @Test
    public void checkPolicyValidateFailIOValidation() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_VALIDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");
        policyContext.setVariable(RESOURCE, POLICY_XML);
        when(policyManager.findByGuid("xxx")).thenReturn(new Policy(PolicyType.INCLUDE_FRAGMENT, "xxx", "<policy>", false));
        PolicyValidatorResult policyValidatorResult = new PolicyValidatorResult();
        List<Integer> list = new ArrayList<Integer>();
        list.add(new Integer(1));
        policyValidatorResult.addError(new PolicyValidatorResult.Error(list, 1, "", new Throwable()));
        when(policyValidator.validate(any(Assertion.class), any(PolicyValidationContext.class), any(AssertionLicense.class))).thenThrow(new InterruptedException(""));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertEquals(new Integer(500), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(policyManager).findByGuid(anyString());
    }

    @Test
    public void checkPolicyUpdateFindFail() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_UPDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");
        policyContext.setVariable(RESOURCE, "<policy></policy>");
        when(policyManager.findByGuid("xxx")).thenThrow(new FindException());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertEquals(new Integer(500), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(policyManager).findByGuid(anyString());
        verify(policyVersionManager, never()).checkpointPolicy(any(Policy.class), anyBoolean(), anyBoolean());
    }

    @Test
    public void checkPolicyUpdateFail() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_UPDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");
        policyContext.setVariable(RESOURCE, POLICY_XML);
        Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "xxx", POLICY_XML, false);
        when(policyManager.findByGuid("xxx")).thenReturn(policy).thenReturn(policy);
        when(policyVersionManager.checkpointPolicy(any(Policy.class), anyBoolean(), anyBoolean())).thenThrow(new UpdateException());
        PolicyValidatorResult policyValidatorResult = new PolicyValidatorResult();
        policyValidatorResult.addWarning(new PolicyValidatorResult.Warning(new ArrayList<Integer>(), 1, "", new Throwable()));
        when(policyValidator.validate(any(Assertion.class), any(PolicyValidationContext.class), any(AssertionLicense.class))).thenReturn(policyValidatorResult);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertEquals(new Integer(500), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        verify(policyManager, times(2)).findByGuid(anyString());
        verify(policyVersionManager).checkpointPolicy(any(Policy.class), anyBoolean(), anyBoolean());
    }

    @Test
    public void checkPolicyUpdateInvalid() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_UPDATE_URI);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
    }

    @Test
    public void checkPolicyUpdateMissingGuid() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_UPDATE_URI);
        policyContext.setVariable(RESOURCE, POLICY_XML);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("GUID is missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
    }

    @Test
    public void checkPolicyUpdateMissingResource() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + POLICY_UPDATE_URI);
        policyContext.setVariable(OPTION_POLICY_GUID, "xxx");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Policy resource XML is missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
    }

    private void assertContextVariableDoesNotExist(final String name) {
        try {
            policyContext.getVariable(name);
            fail("Found unexpected context variable with name=" + name);
        } catch (final NoSuchVariableException e) {
            // expected
        }
    }

    private static final String POLICY_XML = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "<wsp:All wsp:Usage=\"Required\">\n" +
            "<L7p:CommentAssertion>\n" +
            "<L7p:Comment stringValue=\"comment\"/>\n" +
            "</L7p:CommentAssertion>\n" +
            "</wsp:All>\n</wsp:Policy>";
}
