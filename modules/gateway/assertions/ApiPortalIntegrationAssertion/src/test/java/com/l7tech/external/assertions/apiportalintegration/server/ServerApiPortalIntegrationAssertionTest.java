package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.Collections;

import static org.junit.Assert.*;

public class ServerApiPortalIntegrationAssertionTest {
    private ServerApiPortalIntegrationAssertion serverAssertion;
    private ApiPortalIntegrationAssertion assertion;
    private ApplicationContext applicationContext;
    private PolicyEnforcementContext policyContext;
    private TestAudit testAudit;

    @Before
    public void setup() throws Exception {
        applicationContext = ApplicationContexts.getTestApplicationContext();
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        assertion = new ApiPortalIntegrationAssertion();
        serverAssertion = new ServerApiPortalIntegrationAssertion(assertion, applicationContext);
        testAudit = new TestAudit();
    }

    @Test
    public void checkRequest() throws Exception {
        assertion.setApiId("a1");
        assertion.setApiGroup("g1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("a1", (String) policyContext.getVariable("portal.managed.service.apiId"));
        assertEquals("g1", (String) policyContext.getVariable("portal.managed.service.apiGroup"));
    }

    @Test(expected = NoSuchVariableException.class)
    public void checkRequestNullApiId() throws Exception {
        assertion.setApiId(null);
        assertion.setApiGroup("g1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("g1", (String) policyContext.getVariable("portal.managed.service.apiGroup"));

        try {
            policyContext.getVariable("portal.managed.service.apiId");
        } catch (final NoSuchVariableException e) {
            // expected
            throw e;
        }
        fail("Expected NoSuchVariableException");
    }

    @Test(expected = NoSuchVariableException.class)
    public void checkRequestNullApiGroup() throws Exception {
        assertion.setApiId("a1");
        assertion.setApiGroup(null);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("a1", (String) policyContext.getVariable("portal.managed.service.apiId"));

        try {
            policyContext.getVariable("portal.managed.service.apiGroup");
        } catch (final NoSuchVariableException e) {
            // expected
            throw e;
        }
        fail("Expected NoSuchVariableException");
    }

    @Test
    public void checkRequestNonDefaultVariablePrefix() throws Exception {
        assertion.setVariablePrefix("foo");
        assertion.setApiId("a1");
        assertion.setApiGroup("g1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("a1", (String) policyContext.getVariable("foo.apiId"));
        assertEquals("g1", (String) policyContext.getVariable("foo.apiGroup"));
    }

    @Test
    public void checkRequestNullVariablePrefix() throws Exception {
        assertion.setVariablePrefix(null);
        assertion.setApiId("a1");
        assertion.setApiGroup("g1");
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.ASSERTION_MISCONFIGURED));
    }

    @Test
    public void checkRequestEmptyVariablePrefix() throws Exception {
        assertion.setVariablePrefix("   ");
        assertion.setApiId("a1");
        assertion.setApiGroup("g1");
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.ASSERTION_MISCONFIGURED));
    }
}
