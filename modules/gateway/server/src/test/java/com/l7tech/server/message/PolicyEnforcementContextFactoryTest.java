package com.l7tech.server.message;

import com.l7tech.message.Message;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PolicyEnforcementContextFactoryTest {
    private Message request;
    private Message response;

    @Before
    public void setup() {
        request = new Message();
        response = new Message();
    }

    @BugId("SSG-7450")
    @Test
    public void createChildContextReusesRequestId() throws Exception {
        final PolicyEnforcementContext parent = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        final PolicyEnforcementContext child = PolicyEnforcementContextFactory.createUnregisteredPolicyEnforcementContext(parent);
        assertTrue(child instanceof ChildPolicyEnforcementContext);
        assertEquals(parent.getVariable("requestId"), child.getVariable("requestId"));
    }
}
