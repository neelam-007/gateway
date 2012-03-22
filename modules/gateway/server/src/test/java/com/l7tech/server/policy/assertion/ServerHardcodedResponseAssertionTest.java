package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ServerHardcodedResponseAssertionTest {
    private static final int STATUS = 400;
    private HardcodedResponseAssertion assertion;
    private ServerHardcodedResponseAssertion serverAssertion;
    private PolicyEnforcementContext policyContext;
    private TestAudit testAudit;

    @Before
    public void setup() throws Exception {
        assertion = new HardcodedResponseAssertion();
        serverAssertion = new ServerHardcodedResponseAssertion(assertion, ApplicationContexts.getTestApplicationContext());
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
    }

    @Test
    public void checkRequestDefaultStatus() throws Exception {
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(Integer.valueOf(HardcodedResponseAssertion.DEFAULT_STATUS).intValue(), policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestStringStatus() throws Exception {
        assertion.setResponseStatus(String.valueOf(STATUS));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestIntStatus() throws Exception {
        assertion.setResponseStatus(STATUS);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestContextVariableStatus() throws Exception {
        assertion.setResponseStatus("${status}");
        policyContext.setVariable("status", STATUS);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestContextVariableStatusNotAnInteger() throws Exception {
        assertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "notaninteger");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: notaninteger"));
    }

    @Test
    public void checkRequestContextVariableStatusTooLarge() throws Exception {
        assertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "2147483648");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: 2147483648"));
    }

    @Test
    public void checkRequestContextVariableStatusZero() throws Exception {
        assertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "0");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: 0"));
    }

    @Test
    public void checkRequestContextVariableStatusNegative() throws Exception {
        assertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "-1");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: -1"));
    }

    @Test
    public void checkRequestContextVariableStatusDoesNotExist() throws Exception {
        assertion.setResponseStatus("${status}");
        // do not set ${status} on policy context

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: "));
    }

    @Test
    public void checkRequestStatusNull() throws Exception {
        assertion.setResponseStatus(null);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: null"));
    }
}
