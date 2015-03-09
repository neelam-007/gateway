package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HasServiceId;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.ResolveServiceAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerResolveServiceAssertionJavaTest {

    private static final String SERVICE_RESOLUTION_PATH = "/foo";
    private static final String SERVICE_NAME = "testService";
    private static final Goid SERVICE_GOID = new Goid(0, 2424);

    private static PublishedService service;

    @Mock
    private ServiceCache serviceCache;

    private TestAudit audit;
    private ResolveServiceAssertion assertion;
    private ServerResolveServiceAssertion serverAssertion;
    private PolicyEnforcementContext pec;

    @BeforeClass
    public static void init() {
        service = new PublishedService();
        service.setGoid(SERVICE_GOID);
        service.setName(SERVICE_NAME);
        service.setSoap(true);
    }

    @Before
    public void setUp() throws Exception {
        audit = new TestAudit();

        assertion = new ResolveServiceAssertion();
        assertion.setUri(SERVICE_RESOLUTION_PATH);

        serverAssertion = new ServerResolveServiceAssertion(assertion, audit.factory());

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                        .put("auditFactory", audit.factory())
                        .put("serviceCache", serviceCache)
                        .unmodifiableMap()
        );

        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    /**
     * succeed if matching service is present in service cache
     *
     * N.B. consolidated old tests for successful context variable creation into this success case - seemed redundant
     */
    @Test
    public void testCheckRequest_MatchingServicePresent_HasServiceIdKnobAttached() throws Exception {
        when(serviceCache.resolve(SERVICE_RESOLUTION_PATH, null, null)).thenReturn(Collections.singletonList(service));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        verify(serviceCache).resolve(SERVICE_RESOLUTION_PATH, null, null);

        HasServiceId hasServiceIdKnob = pec.getRequest().getKnob(HasServiceId.class);

        assertNotNull(hasServiceIdKnob);
        assertEquals(service.getGoid(), hasServiceIdKnob.getServiceGoid());
        assertEquals(SERVICE_NAME, pec.getVariable("resolvedService.name"));
        assertEquals(SERVICE_GOID.toString(), pec.getVariable("resolvedService.oid"));
        assertEquals(true, pec.getVariable("resolvedService.soap"));
        assertTrue(audit.isAuditPresent(AssertionMessages.RESOLVE_SERVICE_SUCCEEDED));
    }

    /**
     * fail if more than one matching service in service cache
     */
    @Test
    public void testCheckRequest_MultipleMatchingServicesPresent_MultipleFoundAuditedAndAssertionFails() throws Exception {
        when(serviceCache.resolve(SERVICE_RESOLUTION_PATH, null, null))
                .thenReturn(CollectionUtils.list(service, new PublishedService()));

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        verify(serviceCache).resolve(SERVICE_RESOLUTION_PATH, null, null);

        assertNull(pec.getRequest().getKnob(HasServiceId.class));
        assertTrue(audit.isAuditPresent(AssertionMessages.RESOLVE_SERVICE_FOUND_MULTI));
    }

    /**
     * fail if no matching service in service cache
     */
    @Test
    public void testCheckRequest_NoMatchingServicePresent_ServiceNotFoundAuditedAndAssertionFails() throws Exception {
        when(serviceCache.resolve(SERVICE_RESOLUTION_PATH, null, null))
                .thenReturn(Collections.<PublishedService>emptyList());

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        verify(serviceCache).resolve(SERVICE_RESOLUTION_PATH, null, null);

        assertNull(pec.getRequest().getKnob(HasServiceId.class));
        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.RESOLVE_SERVICE_NOT_FOUND));
    }

    /**
     * fail if variable prefix is null
     */
    @Test
    public void testCheckRequest_AssertionVariablePrefixNull_NoPrefixAuditedAndAssertionFails() throws Exception {
        assertion.setPrefix(null);

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertNull(pec.getRequest().getKnob(HasServiceId.class));
        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.RESOLVE_SERVICE_NO_PREFIX));
    }

    /**
     * fail if variable prefix is zero-length string
     */
    @Test
    public void testCheckRequest_AssertionVariablePrefixEmpty_NoPrefixAuditedAndAssertionFails() throws Exception {
        assertion.setPrefix("");

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertNull(pec.getRequest().getKnob(HasServiceId.class));
        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.RESOLVE_SERVICE_NO_PREFIX));
    }

    /**
     * fail if service cache throws ServiceResolutionException
     */
    @Test
    public void testCheckRequest_ServiceCacheThrowsServiceResolutionException_ExceptionAuditedAndAssertionFails() throws Exception {
        when(serviceCache.resolve(SERVICE_RESOLUTION_PATH, null, null))
                .thenThrow(new ServiceResolutionException("HORRIBLE DISASTROUS EXCEPTION OH MY"));

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        verify(serviceCache).resolve(SERVICE_RESOLUTION_PATH, null, null);

        assertNull(pec.getRequest().getKnob(HasServiceId.class));
        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.RESOLVE_SERVICE_FAILED, true));
    }
}