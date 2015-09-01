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
public class ServerManageAccountPlansTest {
    private static final String ORG_IDS = "1,2";
    public static final boolean DEFAULT_PLAN_ENABLED = true;
    public static final boolean THROUGHPUT_QUOTA_ENABLED = true;
    public static final int QUOTA_10 = 10;
    public static final int TIME_UNIT_1 = 1;
    public static final int COUNTER_STRATEGY_1 = 1;
    private static final Date LAST_UPDATE = new Date();
    private static final String INPUT = "<input>dummy</input>";
    private static final String OUTPUT = "<output>dummy</output>";
    private ServerManagePortalResourceAssertion serverAssertion;
    private ManagePortalResourceAssertion assertion;
    private PolicyEnforcementContext policyContext;
    private List<AccountPlanResource> accountPlanResources;
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
        accountPlanResources = new ArrayList<AccountPlanResource>();
        expectedFilters = new HashMap<String, String>();
    }

    @Test
    public void checkRequestGetAccountPlan() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1");
        final AccountPlanResource resource = createAccountPlanResource("p1", "pName", LAST_UPDATE, "policy xml",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource);
        when(accountPlanResourceHandler.get(anyMap())).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanResource.class))).thenReturn("plan xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("plan xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAccountPlanNotFound() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1");
        when(accountPlanResourceHandler.get(anyMap())).thenReturn(Collections.<AccountPlanResource>emptyList());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find AccountPlan with planId=p1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAccountPlans() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        final AccountPlanResource resource1 = createAccountPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        final AccountPlanResource resource2 = createAccountPlanResource("p2", "pName2", LAST_UPDATE, "policy xml 2",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource1);
        accountPlanResources.add(resource2);
        when(accountPlanResourceHandler.get(anyMap())).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenReturn("plans xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource1, resource2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("plans xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAllAccountPlansNone() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        accountPlanResources.clear();
        when(accountPlanResourceHandler.get(anyMap())).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenReturn("plans xml");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("success", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("plans xml", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestGetAccountPlanFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1");
        when(accountPlanResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAccountPlanJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1");
        final AccountPlanResource resource = createAccountPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource);
        when(accountPlanResourceHandler.get(anyMap())).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        expectedFilters.put("id", "p1");
        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAccountPlanTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler, never()).get(anyMap());
        verify(resourceMarshaller, never()).marshal(any(AccountPlanResource.class));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: GET - " + ROOT_URI + "account/plans/p1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAccountPlansFindException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        when(accountPlanResourceHandler.get(anyMap())).thenThrow(new FindException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller, never()).marshal(any(AccountPlanListResource.class));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestGetAllAccountPlansJAXBException() throws Exception {
        policyContext.setVariable(OPERATION, "GET");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        final AccountPlanResource resource = createAccountPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource);
        when(accountPlanResourceHandler.get(anyMap())).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler).get(expectedFilters);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansSaveException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        accountPlanResources.add(createAccountPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(new AccountPlanListResource(accountPlanResources));
        when(accountPlanResourceHandler.put(anyList(), anyBoolean())).thenThrow(new SaveException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler).put(accountPlanResources, false);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansUnmarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler,never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansMarshalException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        final AccountPlanResource resource = createAccountPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource);
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(new AccountPlanListResource(accountPlanResources));
        when(accountPlanResourceHandler.put(accountPlanResources, false)).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenThrow(new JAXBException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler).put(accountPlanResources, false);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals("N/A", (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansEmptyPlanId() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        accountPlanResources.add(createAccountPlanResource("", "pName1", null, "policy xml 1",
                !DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(new AccountPlanListResource(accountPlanResources));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler, never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Resource id missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansNullPlanId() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        accountPlanResources.add(createAccountPlanResource(null, "pName1", null, "policy xml 1",
                !DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(new AccountPlanListResource(accountPlanResources));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler, never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Resource id missing", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/extra");
        policyContext.setVariable(RESOURCE, INPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller, never()).unmarshal(anyString(), Matchers.<Class>any());
        verify(accountPlanResourceHandler, never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: PUT - /1/account/plans/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlans() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        final AccountPlanResource resource1 = createAccountPlanResource("p1", "pName1", null, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource1);
        final AccountPlanResource resource2 = createAccountPlanResource("p2", "pName2", null, "policy xml 2",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource2);
        final AccountPlanListResource listResource = new AccountPlanListResource(accountPlanResources);
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(listResource);
        when(accountPlanResourceHandler.put(accountPlanResources, false)).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler).put(accountPlanResources, false);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource1, resource2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansNone() throws Exception {
        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"></l7:AccountPlans>";
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, xml);
        accountPlanResources.clear();
        final AccountPlanListResource listResource = new AccountPlanListResource(accountPlanResources);
        when(resourceUnmarshaller.unmarshal(xml, AccountPlanListResource.class)).thenReturn(listResource);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenReturn(xml);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(xml, AccountPlanListResource.class);
        verify(accountPlanResourceHandler,never()).put(anyList(), anyBoolean());
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(xml, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @BugId("APIM-522")
    @Test
    public void checkRequestAddOrUpdateAccountPlansNoneRemoveOmitted() throws Exception {
        final String xml = "<l7:AccountPlans xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\"></l7:AccountPlans>";
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, xml);
        policyContext.setVariable(OPTION_REMOVE_OMITTED, "true");
        accountPlanResources.clear();
        final AccountPlanListResource listResource = new AccountPlanListResource(accountPlanResources);
        when(resourceUnmarshaller.unmarshal(xml, AccountPlanListResource.class)).thenReturn(listResource);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenReturn(xml);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(xml, AccountPlanListResource.class);
        verify(accountPlanResourceHandler).put(accountPlanResources, true);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher()));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(xml, policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestAddOrUpdatAccountPlansUpdateException() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        accountPlanResources.add(createAccountPlanResource("p1", "pName1", null, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(new AccountPlanListResource(accountPlanResources));
        when(accountPlanResourceHandler.put(accountPlanResources, false)).thenThrow(new UpdateException("mocking exception"));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler).put(accountPlanResources, false);
        verify(resourceMarshaller, never()).marshal(Matchers.<Resource>any());
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestAddOrUpdateAccountPlansDeleteOmitted() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        policyContext.setVariable(OPTION_REMOVE_OMITTED, true);
        final AccountPlanResource resource1 = createAccountPlanResource("p1", "pName1", null, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource1);
        final AccountPlanResource resource2 = createAccountPlanResource("p2", "pName2", null, "policy xml 2",
                !DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource2);
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(new AccountPlanListResource(accountPlanResources));
        when(accountPlanResourceHandler.put(accountPlanResources, true)).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler).put(accountPlanResources, true);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource1, resource2)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    /**
     * If option is not a valid boolean, should not delete any plans.
     */
    @Test
    public void checkRequestAddOrUpdateAccountPlansDeleteOmittedNotValidBoolean() throws Exception {
        policyContext.setVariable(OPERATION, "PUT");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");
        policyContext.setVariable(RESOURCE, INPUT);
        policyContext.setVariable(OPTION_REMOVE_OMITTED, "invalid");
        final AccountPlanResource resource = createAccountPlanResource("p1", "pName1", null, "policy xml 1",
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        accountPlanResources.add(resource);
        when(resourceUnmarshaller.unmarshal(INPUT, AccountPlanListResource.class)).thenReturn(new AccountPlanListResource(accountPlanResources));
        when(accountPlanResourceHandler.put(accountPlanResources, false)).thenReturn(accountPlanResources);
        when(resourceMarshaller.marshal(any(AccountPlanListResource.class))).thenReturn(OUTPUT);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(resourceUnmarshaller).unmarshal(INPUT, AccountPlanListResource.class);
        verify(accountPlanResourceHandler).put(accountPlanResources, false);
        verify(resourceMarshaller).marshal(argThat(new AccountPlanListResourceMatcher(resource)));
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertEquals(OUTPUT, (String) policyContext.getVariable(RESPONSE_RESOURCE));
    }

    @Test
    public void checkRequestDeleteAccountPlan() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler).delete("p1");
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(200), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals(SUCCESS, (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteAccountPlanDeleteException() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1");
        doThrow(new DeleteException("mocking exception")).when(accountPlanResourceHandler).delete("p1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler).delete("p1");
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertContextVariableDoesNotExist(RESPONSE_STATUS);
        assertContextVariableDoesNotExist(RESPONSE_DETAIL);
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteAccountPlanNotFound() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1");
        doThrow(new FindException("mocking exception")).when(accountPlanResourceHandler).delete("p1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler).delete("p1");
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(404), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Cannot find AccountPlan with planId=p1", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteAccountPlanTooManyTokens() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans/p1/extra");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler, never()).delete(anyString());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: DELETE - /1/account/plans/p1/extra", (String) policyContext.getVariable(RESPONSE_DETAIL));
        assertContextVariableDoesNotExist(RESPONSE_RESOURCE);
    }

    @Test
    public void checkRequestDeleteAccountPlanMissingPlanId() throws Exception {
        policyContext.setVariable(OPERATION, "DELETE");
        policyContext.setVariable(RESOURCE_URI, ROOT_URI + "account/plans");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        verify(accountPlanResourceHandler, never()).delete(anyString());
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(new Integer(400), (Integer) policyContext.getVariable(RESPONSE_STATUS));
        assertEquals("Invalid operation and/or resourceUri: DELETE - /1/account/plans", (String) policyContext.getVariable(RESPONSE_DETAIL));
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

    private class AccountPlanListResourceMatcher extends ArgumentMatcher<AccountPlanResource> {
        final List<AccountPlanResource> expectedPlans;

        public AccountPlanListResourceMatcher(final AccountPlanResource... resources) {
            this.expectedPlans = Arrays.asList(resources);
        }

        @Override
        public boolean matches(final Object o) {
            final AccountPlanListResource list = (AccountPlanListResource) o;
            if (expectedPlans.size() == list.getAccountPlans().size() && expectedPlans.containsAll(list.getAccountPlans())) {
                return true;
            }
            return false;
        }
    }

    private AccountPlanResource createAccountPlanResource(final String planId, final String planName,
                                                          final Date lastUpdate, final String policyXml, final boolean defaultPlan,
                                                          final boolean throughputQuotaEnabled, final int quota, final int timeUnit,
                                                          final int counterStrategy, final String organizationIds) {
        final AccountPlanResource resource = new AccountPlanResource();
        resource.setPlanId(planId);
        resource.setPlanName(planName);
        resource.setLastUpdate(lastUpdate);
        resource.setPolicyXml(policyXml);
        resource.setDefaultPlan(defaultPlan);
        final PlanDetails planDetails = new PlanDetails();
        final ThroughputQuotaDetails throughputQuotaDetails = new ThroughputQuotaDetails();
        throughputQuotaDetails.setEnabled(throughputQuotaEnabled);
        throughputQuotaDetails.setQuota(quota);
        throughputQuotaDetails.setTimeUnit(timeUnit);
        throughputQuotaDetails.setCounterStrategy(counterStrategy);
        planDetails.setThroughputQuota(throughputQuotaDetails);
        resource.setPlanDetails(planDetails);
        final AccountPlanMapping organizations = new AccountPlanMapping();
        organizations.setIds(organizationIds);
        resource.setPlanMapping(organizations);
        return resource;
    }
}
