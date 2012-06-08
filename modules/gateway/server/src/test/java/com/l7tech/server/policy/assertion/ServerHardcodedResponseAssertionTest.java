package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
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
    private PolicyEnforcementContext policyContext;
    private TestAudit testAudit;

    @Before
    public void setup() throws Exception {
        assertion = new HardcodedResponseAssertion();
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        testAudit = new TestAudit();
    }

    @Test
    public void checkRequestDefaultStatus() throws Exception {
        final AssertionStatus assertionStatus = getServerAssertion(assertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(Integer.valueOf(HardcodedResponseAssertion.DEFAULT_STATUS).intValue(), policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestStringStatus() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus(String.valueOf(STATUS));
        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestIntStatus() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus(STATUS);

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestContextVariableStatus() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", STATUS);

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestContextVariableStatusNotAnInteger() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "notaninteger");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: notaninteger"));
    }

    @Test
    public void checkRequestContextVariableStatusTooLarge() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "2147483648");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: 2147483648"));
    }

    @Test
    public void checkRequestContextVariableStatusZero() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "0");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: 0"));
    }

    @Test
    public void checkRequestContextVariableStatusNegative() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "-1");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: -1"));
    }

    @Test
    public void checkRequestContextVariableStatusDoesNotExist() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        // do not set ${status} on policy context

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: "));
    }

    private ServerHardcodedResponseAssertion getServerAssertion(final HardcodedResponseAssertion assertion) throws PolicyAssertionException {
        final ServerHardcodedResponseAssertion serverAssertion = new ServerHardcodedResponseAssertion(assertion, ApplicationContexts.getTestApplicationContext());
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
        return serverAssertion;
    }
}
